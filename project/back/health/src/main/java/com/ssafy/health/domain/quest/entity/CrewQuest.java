package com.ssafy.health.domain.quest.entity;

import com.ssafy.health.common.entity.BaseEntity;
import com.ssafy.health.domain.crew.entity.Crew;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewQuest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private QuestStatus status;

    @ManyToOne
    @JoinColumn(name = "crew_id")
    private Crew crew;

    @ManyToOne
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @Builder
    public CrewQuest(Crew crew, Quest quest) {
        this.crew = crew;
        this.quest = quest;
        this.status = QuestStatus.CREATED;
    }

    public void updateStatus(QuestStatus status) {
        this.status = status;
    }
}
