import { useState } from 'react';
import GeneralButton from '@/components/Button/GeneralButton';
import ExerciseModal from '@/components/Exercise/ExerciseModal';
import RankingImg from '@/assets/image/ranking.png';
import CrewAndMemberList from '@/components/Crew/CrewAndMemberList';
import { getExerciseCrewRanking } from '@/api/crew';
import './CrewRanking.scss';
import { useQuery } from '@tanstack/react-query';
import querykeys from '@/utils/querykeys';

export default function CrewRankingPage() {
  const [exerciseId, setExerciseId] = useState<number | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [_, setSelectedExercise] = useState<{ id: number; name: string } | null>(null);

  const handleSelectExercise = (selected: { id: number; name: string } | { id: number; name: string }[]) => {
    if (Array.isArray(selected)) {
      setSelectedExercise(selected[0]);
      setExerciseId(selected[0].id);
    } else {
      setSelectedExercise(selected);
      setExerciseId(selected.id);
    }
    setShowModal(false);
  };

  const handleCloseModal = () => {
    setShowModal(false);
  };

  const { data: exerciseCrewRanking, isSuccess } = useQuery({
    queryKey: [querykeys.EXERCISE_RANKING, exerciseId],
    queryFn: () => getExerciseCrewRanking(Number(exerciseId)),
    enabled: !!exerciseId, // exerciseId가 있을 때만 실행
  });

  return (
    <div className="realTimeRankingContainer">
      <p className="realTimeRankingTitle">실시간 크루 랭킹</p>

      {showModal && (
        <ExerciseModal onSelectExercise={handleSelectExercise} multiple={false} onClose={handleCloseModal} />
      )}

      {!exerciseId ? (
        <GeneralButton
          buttonStyle={{ style: 'primary', size: 'large' }}
          onClick={() => setShowModal(true)}
          className="selectExercise">
          운동 종목 선택하기
        </GeneralButton>
      ) : (
        isSuccess &&
        (exerciseCrewRanking?.data?.data?.crewList?.length > 0 ? (
          <CrewAndMemberList type="crew" data={exerciseCrewRanking.data.data.crewList} />
        ) : (
          <p>선택한 운동의 크루 랭킹이 없습니다.</p>
        ))
      )}

      <img src={RankingImg} alt="realTimeRankingBackground" className="realTimeRankingBackground" />
    </div>
  );
}
