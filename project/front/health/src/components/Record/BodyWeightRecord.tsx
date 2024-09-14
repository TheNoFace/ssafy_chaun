import PredictionGraph from '@/components/Record/PredictionGraph';
import './BodyWeihtRecord.scss';
import predictionImage1 from '@/assets/image/model.png'

interface BodyWeightRecordProps {
  data: { time: string; weight: number }[];
}

export default function BodyWeightRecord({ data }: BodyWeightRecordProps) {
  return (
    <div className="bodyWeightRecord">
      <div className="predictionCards">
        <div className="predictionCard">
            <img src={predictionImage1} alt="predictionImage1" className="predictionImage__1" />
            <p className="time">현재</p>
        </div>
        <div className="predictionCard">
            <img src={predictionImage1} alt="predictionImage1" className="predictionImage__2" />
            <p className="time">1달 후</p>
        </div>
        <div className="predictionCard">
            <img src={predictionImage1} alt="predictionImage1" className="predictionImage__3" />
            <p className="time">3달 후</p>
        </div>
      </div>

      <div className="predictionGraph">
        <PredictionGraph data={data} />
      </div>
    </div>
  );
}
