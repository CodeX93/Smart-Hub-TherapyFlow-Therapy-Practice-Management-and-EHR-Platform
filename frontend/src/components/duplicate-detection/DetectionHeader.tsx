import { Info, RotateCw } from "lucide-react";
import { Button } from "../ui/button";

interface DetectionHeaderProps {
  count: number;
  affectedRecords: number;
  onRefresh: () => void;
}

const DetectionHeader = ({
  count,
  affectedRecords,
  onRefresh,
}: DetectionHeaderProps) => {
  return (
    <div className="flex items-center justify-between p-4 bg-(--neutral-100) rounded-2xl shadow-xs mb-6">
      <div className="flex items-center gap-3">
        <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-white text-(--text-primary-500)">
          <Info size={20} />
        </div>
        <div>
          <h2 className="font-semibold text-(--text-primary-dark)">
            Detection Results
          </h2>
          <p className="text-sm text-(--text-neutral-600)">
            Found{" "}
            <span className="font-bold text-(--text-primary-dark)">
              {count}
            </span>{" "}
            potential duplicate groups affecting{" "}
            <span className="font-bold text-(--text-primary-dark)">
              {affectedRecords}
            </span>{" "}
            client records. Review each group below and mark duplicates as
            needed.
          </p>
        </div>
      </div>
      <Button
        onClick={onRefresh}
        variant="link"
        className="flex items-center gap-2 px-4 py-2 text-sm font-semibold text-(--text-primary-500) hover:text-(--text-primary-500) rounded-full transition-colors cursor-pointer"
      >
        <RotateCw size={18} className="text-(--text-primary-500)" />
        Refresh Scan
      </Button>
    </div>
  );
};

export default DetectionHeader;
