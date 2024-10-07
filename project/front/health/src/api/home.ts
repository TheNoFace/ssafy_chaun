// import axios from 'axios';
import exportAxios from './axiosInstance';
const baseUrl = import.meta.env.VITE_APP_BASE_URL;
interface ExerciseTimeResponse {
  dailyAccumulatedExerciseTime: number;
  weeklyAccumulatedExerciseTime: number;
}

interface ExerciseRecord {
  id: number;
  exerciseDuration: number;
  burnedCalories: number;
  exerciseName: string;
  createdAt: string;
  data?: ExerciseRecord;
}

// 본인의 이번주, 오늘 운동시간 조회
export const exerciseTime = async (): Promise<ExerciseTimeResponse> => {
  const response = await exportAxios.get<ExerciseTimeResponse>(`${baseUrl}/users/my/exercise-time`);

  return response.data;
};

// 자신의 특정 주의 운동 기록 조회
export const exerciseRecord = async (year: number, month: number, week: number): Promise<ExerciseRecord> => {
  const response = await exportAxios.get<ExerciseRecord>(`${baseUrl}/users/exercise-history/week`, {
    params: {
      year,
      month,
      week,
    },
  });
  if (response.data.data !== undefined) {
    return response.data.data;
  }
  console.log('exerciseRecord', response);
  return response.data;
};
