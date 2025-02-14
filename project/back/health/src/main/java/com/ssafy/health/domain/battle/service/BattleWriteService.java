package com.ssafy.health.domain.battle.service;

import com.ssafy.health.domain.account.entity.User;
import com.ssafy.health.domain.account.entity.UserCrew;
import com.ssafy.health.domain.account.repository.UserCrewRepository;
import com.ssafy.health.domain.account.repository.UserRepository;
import com.ssafy.health.domain.battle.dto.response.BattleAndCrewDto;
import com.ssafy.health.domain.battle.dto.response.BattleMatchResponseDto;
import com.ssafy.health.domain.battle.entity.Battle;
import com.ssafy.health.domain.battle.entity.BattleStatus;
import com.ssafy.health.domain.battle.entity.RankHistory;
import com.ssafy.health.domain.battle.repository.BattleRepository;
import com.ssafy.health.domain.battle.repository.RankHistoryRepository;
import com.ssafy.health.domain.coin.service.CoinService;
import com.ssafy.health.domain.coin.service.CoinValidator;
import com.ssafy.health.domain.crew.entity.Crew;
import com.ssafy.health.domain.crew.exception.CrewNotFoundException;
import com.ssafy.health.domain.crew.repository.CrewRepository;
import com.ssafy.health.domain.notification.dto.request.NotificationRequestDto;
import com.ssafy.health.domain.notification.entity.NotificationType;
import com.ssafy.health.domain.notification.service.NotificationWriteService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static com.ssafy.health.domain.coin.CoinCost.START_BATTLE;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BattleWriteService {

    private final CoinService coinService;
    private final CrewRepository crewRepository;
    private final BattleRepository battleRepository;
    private final UserCrewRepository userCrewRepository;
    private final RankHistoryRepository rankHistoryRepository;
    private final BattleValidator battleValidator;
    private final CoinValidator coinValidator;
    private final NotificationWriteService notificationWriteService;
    private final UserRepository userRepository;

    public BattleMatchResponseDto startBattle(Long crewId) {
        battleValidator.validateBattleAlreadyExists(crewId);

        Crew myCrew = crewRepository.findById(crewId).orElseThrow(CrewNotFoundException::new);
        coinValidator.validateSufficientCoins(myCrew.getCrewCoin(), START_BATTLE.getAmount());

        Float myCrewScore = findRecentBattleScore(crewId);
        Crew opponentCrew = findOpponentCrew(crewId, myCrewScore);

        coinService.spendCrewCoins(myCrew, START_BATTLE.getAmount());
        coinService.spendCrewCoins(opponentCrew, START_BATTLE.getAmount());

        Battle battle = battleRepository.save(Battle.builder()
                .homeCrew(myCrew)
                .awayCrew(opponentCrew)
                .build());

        List<User> homeCrewMembers = userRepository.findUserByCrewId(myCrew.getId());
        List<User> awayCrewMembers = userRepository.findUserByCrewId(opponentCrew.getId());

        List<NotificationRequestDto> notificationRequestDtoList = new ArrayList<>();
        notificationRequestDtoList.addAll(createNotification(battle, homeCrewMembers, myCrew));
        notificationRequestDtoList.addAll(createNotification(battle, awayCrewMembers, opponentCrew));

        sendNotification(notificationRequestDtoList);

        return BattleMatchResponseDto.builder()
                .battleId(battle.getId())
                .battleStatus(battle.getStatus())
                .exerciseName(myCrew.getExercise().getName())
                .myCrewName(myCrew.getName())
                .myCrewScore(0F)
                .opponentCrewName(opponentCrew.getName())
                .opponentCrewScore(0F)
                .dDay(calculateDDay())
                .build();
    }

    @Scheduled(cron = "0 30 4 ? * MON")
    public void finishBattles() {
        List<Battle> ongoingBattles = battleRepository.findByStatus(BattleStatus.STARTED);
        List<NotificationRequestDto> notificationRequestDtoList = new ArrayList<>();

        ongoingBattles.forEach(battle -> {
            battle.finishBattle();
            battleRepository.save(battle);

            Crew[] crews = determineWinningCrew(battle);
            Crew winningCrew = crews[0];
            List<UserCrew> winningUserCrewList = findUserCrewOrderByScore(winningCrew);
            List<User> winningCrewMemberList = winningUserCrewList.stream().map(UserCrew::getUser).toList();
            List<User> remainingMembers;
            if (winningCrewMemberList.size() > 3) {
                remainingMembers = winningCrewMemberList.subList(3, winningCrewMemberList.size());
            } else {
                remainingMembers = Collections.emptyList();
            }

            HashMap<User, Integer> topMemberCoinResults = coinService.distributeBattleRewards(winningCrewMemberList,
                    winningUserCrewList);
            try {
                notificationRequestDtoList.addAll(createTopMemberNotification(battle, topMemberCoinResults, winningCrew));
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            coinService.grantCoinsToCrew(winningCrew, 200);

            saveRankHistory(winningUserCrewList, winningCrew);
            resetScore(winningCrew, winningUserCrewList);

            Crew losingCrew = crews[1];
            List<UserCrew> losingUserCrewList = findUserCrewOrderByScore(losingCrew);
            List<User> losingCrewMemberList = losingUserCrewList.stream().map(UserCrew::getUser).toList();

            saveRankHistory(losingUserCrewList, losingCrew);
            resetScore(losingCrew, losingUserCrewList);

            notificationRequestDtoList.addAll(createNotification(battle, remainingMembers, winningCrew));
            notificationRequestDtoList.addAll(createNotification(battle, losingCrewMemberList, losingCrew));

        });

        sendNotification(notificationRequestDtoList);
    }

    private List<NotificationRequestDto> createNotification(Battle battle, List<User> winningCrewMemberList, Crew winningCrew) {
        List<NotificationRequestDto> requestDtoList = winningCrewMemberList.stream().map(user -> {
            try {
                return notificationWriteService.createBattleNotification(
                        NotificationType.BATTLE, user, battle, winningCrew, 0);
            } catch (ExecutionException | InterruptedException e) {
                System.err.println("Failed to create notification: " + e.getMessage());
                return null;
            }
        }).toList();

        return requestDtoList;
    }

    private List<NotificationRequestDto> createTopMemberNotification(Battle battle, Map<User, Integer> topMemberMap,
                                                                     Crew winningCrew)
            throws ExecutionException, InterruptedException {
        List<NotificationRequestDto> notificationRequestDtoList = new ArrayList<>();
        for (User user : topMemberMap.keySet()) {
            notificationRequestDtoList.add(notificationWriteService.
                    createBattleNotification(NotificationType.BATTLE, user, battle,  winningCrew, topMemberMap.get(user)));
        }
        return notificationRequestDtoList;
    }

    private void sendNotification(List<NotificationRequestDto> notificationRequestDtoList) {
        notificationWriteService.saveNotification(notificationRequestDtoList);
    }

    private Crew[] determineWinningCrew(Battle battle) {
        if (battle.getHomeCrewScore() > battle.getAwayCrewScore()) {
            return new Crew[]{battle.getHomeCrew(), battle.getAwayCrew()};
        }
        return new Crew[]{battle.getAwayCrew(), battle.getHomeCrew()};
    }

    private List<UserCrew> findUserCrewOrderByScore(Crew crew) {
        return userCrewRepository.findUserByCrewIdOrderByScore(crew.getId());
    }

    private void saveRankHistory(List<UserCrew> userCrewList, Crew crew) {
        List<RankHistory> rankHistoryList = new ArrayList<>();
        for (int i = 0; i < userCrewList.size(); i++) {
            UserCrew userCrew = userCrewList.get(i);
            rankHistoryList.add(RankHistory.builder()
                    .user(userCrew.getUser())
                    .crew(crew)
                    .ranking(i + 1)
                    .basicScore(userCrew.getBasicScore())
                    .activityScore(userCrew.getActivityScore())
                    .endDate(LocalDate.now().minusDays(1))
                    .build());
        }
        rankHistoryRepository.saveAll(rankHistoryList);
    }

    private void resetScore(Crew crew, List<UserCrew> userCrew) {
        resetCrewScore(crew);
        resetUserCrewScore(userCrew);
    }

    private void resetCrewScore(Crew crew) {
        crew.resetScores();
    }

    private void resetUserCrewScore(List<UserCrew> userCrewList) {
        userCrewList.forEach(UserCrew::resetScores);
    }

    private Integer calculateDDay() {
        LocalDate now = LocalDate.now();
        LocalDate lastDay = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        return (int) ChronoUnit.DAYS.between(now, lastDay) - 1;
    }

    private Crew findOpponentCrew(Long crewId, Float myCrewScore) {
        List<BattleAndCrewDto> recentBattleList = findRecentBattlesForAvailableCrews(crewId);
        return getClosestCrew(myCrewScore, recentBattleList);
    }

    private static Crew getClosestCrew(Float myCrewScore, List<BattleAndCrewDto> recentBattleList) {
        Crew closestCrew = null;
        float smallestDifference = Float.MAX_VALUE;

        for (BattleAndCrewDto dto : recentBattleList) {
            Battle battle = dto.getBattle();

            if (battle == null) {
                continue;
            }
            Crew crew = dto.getCrew();

            Float crewScore = battle.getHomeCrew().equals(dto.getCrew()) ?
                    battle.getHomeCrewScore() :
                    battle.getAwayCrewScore();

            float scoreDifference = Math.abs(myCrewScore - crewScore);

            if (scoreDifference < smallestDifference) {
                smallestDifference = scoreDifference;
                closestCrew = crew;
            }
        }

        if (closestCrew == null && !recentBattleList.isEmpty()) {
            Random random = new Random();
            closestCrew = recentBattleList.get(random.nextInt(recentBattleList.size())).getCrew();
        }
        return closestCrew;
    }

    private Float findRecentBattleScore(Long crewId) {
        Optional<Battle> battle = battleRepository.findFirstByHomeCrewIdOrAwayCrewIdOrderByCreatedAtDesc(crewId, crewId);
        return battle.map(b -> b.getHomeCrew().getId().longValue() == crewId ? b.getHomeCrewScore() : b.getAwayCrewScore())
                .orElse(0F);
    }

    private List<BattleAndCrewDto> findRecentBattlesForAvailableCrews(Long crewId) {
        log.info("findRecentBattlesForAvailableCrews 진입");
        List<Crew> availableCrews = crewRepository.findBattleReadyCrews(crewId);
        log.info("availableCrew 개수는 " + availableCrews.size());
        List<BattleAndCrewDto> battleAndCrewDtoList = new ArrayList<>();

        for (Crew crew : availableCrews) {
            Optional<Battle> findBattle = battleRepository.findFirstByHomeCrewOrAwayCrewOrderByCreatedAtDesc(crew,
                    crew);

            findBattle.ifPresentOrElse(battle -> {
                Crew currentCrew = battle.getAwayCrew().equals(crew) ? battle.getAwayCrew() : battle.getHomeCrew();
                battleAndCrewDtoList.add(new BattleAndCrewDto(currentCrew, battle));
            }, () -> {

                battleAndCrewDtoList.add(new BattleAndCrewDto(crew, null));
            });
        }

        for (BattleAndCrewDto dto : battleAndCrewDtoList) {
            log.info("크루 아이디는 " + dto.getCrew().getId().toString());
            if (dto.getBattle() == null) {
                continue;
            }
            System.out.println(dto.getBattle().getCreatedAt());
        }

        return battleAndCrewDtoList;
    }

}
