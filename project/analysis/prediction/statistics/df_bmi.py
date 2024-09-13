import numpy as np
import pandas as pd

# 신장과 체중을 상태별 인원수에 맞게 생성하는 함수
def generate_height_weight(total_state_data, bmi_data):
    result_data = []

    for (age, sex), state_info in total_state_data.items():
        height_avg = bmi_data.get((age, sex), {}).get('height_avg', 0)
        height_std = bmi_data.get((age, sex), {}).get('height_std', 0)

        # 각 상태에 대한 인원수 가져오기
        state_counts = {
            'state1': int(state_info['new_state1_count']),
            'state2': int(state_info['new_state2_count']),
            'state3': int(state_info['new_state3_count']),
            'state4': int(state_info['new_state4_count'])
        }

        for state, count in state_counts.items():
            # 해당 상태별로 인원수만큼 신장과 체중 생성
            for _ in range(count):
                height = np.random.normal(height_avg, height_std) if height_avg > 0 and height_std > 0 else height_avg
                height_in_m = round(height / 100, 2)  # cm -> m
                
                # 상태에 따른 BMI 생성 (여기서는 임의로 정했지만 상태에 맞게 조정 가능)
                if state == 'state1':  # 예: 상태 1은 정상 체중
                    bmi = np.random.uniform(17.0, 18.5)
                elif state == 'state2':  # 예: 상태 2는 과체중
                    bmi = np.random.uniform(18.5, 22.9)
                elif state == 'state3':  # 예: 상태 3은 비만
                    bmi = np.random.uniform(23.0, 24.9)
                else:  # 상태 4는 고도 비만
                    bmi = np.random.uniform(25.0, 40.0)
                
                bmi = round(bmi, 2)

                # BMI를 기반으로 체중 계산
                weight = round(bmi * (height_in_m ** 2), 2)
                
                result_data.append({
                    'age': age,
                    'sex': sex,
                    'height': height,
                    'weight': weight,
                    'BMI': bmi
                })

    return pd.DataFrame(result_data)

# 상태별 인원수 계산 함수
def calculate_new_counts(row, user_count_dict):
    total_count = user_count_dict.get((row['age'], row['sex']), 0)
    
    # 상태별 인원수 계산
    row['new_state1_count'] = np.floor(total_count * row['state1'] / 100)
    row['new_state2_count'] = np.floor(total_count * row['state2'] / 100)
    row['new_state3_count'] = np.floor(total_count * row['state3'] / 100)
    row['new_state4_count'] = np.floor(total_count * row['state4'] / 100)
    
    # 할당되지 않은 인원수 계산 및 배분
    assigned_count = row['new_state1_count'] + row['new_state2_count'] + row['new_state3_count'] + row['new_state4_count']
    remaining_count = int(total_count - assigned_count)
    
    state_percentages = {
        'new_state1_count': row['state1'],
        'new_state2_count': row['state2'],
        'new_state3_count': row['state3'],
        'new_state4_count': row['state4']
    }
    
    sorted_states = sorted(state_percentages, key=state_percentages.get, reverse=True)
    
    for state in sorted_states:
        if remaining_count > 0:
            row[state] += 1
            remaining_count -= 1
    
    return row

# 데이터를 처리하는 전체 프로세스
def generate_data(row_data, stat_obese):
    # 사용자 데이터를 그룹화
    grouped_user_data = row_data.groupby(['age', 'sex']).size().reset_index(name='user_count')
    user_count_dict = grouped_user_data.set_index(['age', 'sex'])['user_count'].to_dict()

    # 상태별 인원수 계산
    state_distribution_data = stat_obese[['age', 'sex', 'state1', 'state2', 'state3', 'state4']].copy()
    state_distribution_data = state_distribution_data.apply(calculate_new_counts, axis=1, user_count_dict=user_count_dict)

    # 상태별 인원수 저장
    total_state_data = state_distribution_data.set_index(['age', 'sex']).to_dict('index')

    # 신장 평균과 표준편차 데이터 준비
    height_data = stat_obese.set_index(['age', 'sex'])[['height_avg', 'height_std']].to_dict('index')

    # 신장과 체중을 생성
    generated_data = generate_height_weight(total_state_data, height_data)

    return generated_data

# output.csv를 받아와서 BMI를 반영한 데이터 덮어쓰기를 진행하자.
file_path = './output.csv'
data = pd.read_csv(file_path)
# 통계 파일 불러오기
file_path = './국민 건강 통계/obesity.csv'
stats = pd.read_csv(file_path)

# 데이터 출력 이후, 나이와 성별로 정렬
result = generate_data(data, stats)

result.to_csv('updated_output.csv', index=False)
print(f'{len(result)}개의 개인 체형 정보 DB가 수정됐습니다!')
