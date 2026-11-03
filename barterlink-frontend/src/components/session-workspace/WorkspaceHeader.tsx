import WorkspaceView from "@/models/collaboration/WorkspaceView";

interface WorkspaceHeaderProps {
    currentView: WorkspaceView;
    onViewChange: (view: WorkspaceView) => void;
    onToggleInfo: () => void;
}

function WorkspaceHeader({
    currentView,
    onViewChange,
    onToggleInfo,
}: WorkspaceHeaderProps) {

const workspaceItems = [
    {
        label: "Chat",
        view: WorkspaceView.CHAT,
        icon: "💬",
    },
    {
        label: "Video",
        view: WorkspaceView.VIDEO,
        icon: "🎥",
    },
    {
        label: "Resources",
        view: WorkspaceView.RESOURCES,
        icon: "📁",
    },
];

    return (
    <div className="flex items-center justify-between border-b p-4">

        <div className="flex gap-2">
            {workspaceItems.map((item) => {

                const active =
                    currentView === item.view;

                return (
                    <button
                        key={item.view}
                        onClick={() => onViewChange(item.view)}
                        className={`rounded-lg px-4 py-2 transition-colors ${
                            active
                                ? "bg-primary text-primary-foreground"
                                : "border hover:bg-muted"
                        }`}
                    >
                        {item.icon} {item.label}
                    </button>
                );
            })}
        </div>

        <div className="flex gap-2">

            <button
                onClick={onToggleInfo}
                className="rounded-lg border px-4 py-2 hover:bg-muted"
            >
                ℹ Info
            </button>

        </div>

    </div>
);
}

export default WorkspaceHeader;