import chatBubble from "@/assets/figma/comments-empty/chat-bubble.svg";
import chatMessageLines from "@/assets/figma/comments-empty/chat-message-lines.svg";
import illustrationBaseline from "@/assets/figma/comments-empty/illustration-baseline.svg";
import illustrationShadow from "@/assets/figma/comments-empty/illustration-shadow.svg";

const EmptyComments = () => {
    return (
        <div className="flex flex-col items-center justify-center gap-4 text-center">
            <div className="relative h-[4.25rem] w-[7.5rem]" aria-hidden="true">
                <img
                    src={illustrationBaseline}
                    alt=""
                    className="absolute left-0 top-11 h-6 w-[7.5rem]"
                />
                <div className="absolute left-8 top-0 z-10 size-14">
                    <img
                        src={chatBubble}
                        alt=""
                        className="absolute left-[0.291875rem] top-[0.291875rem] h-[2.916875rem] w-[2.916875rem]"
                    />
                    <img
                        src={chatMessageLines}
                        alt=""
                        className="absolute left-[1.0575rem] top-[1.203125rem] h-[0.729375rem] w-[1.385625rem]"
                    />
                </div>
                <img
                    src={illustrationShadow}
                    alt=""
                    className="absolute left-[1.456875rem] top-[2.869375rem] z-20 h-[1.27375rem] w-[4.649375rem]"
                />
            </div>
            <div>
                <h4 className="text-xl font-semibold leading-7 text-[#1B1C20]">
                    No comments yet
                </h4>
                <p className="mt-1 text-base leading-6 text-[#5B616E]">
                    Be the first to add a comment to track progress
                </p>
            </div>
        </div>
    );
};

export default EmptyComments;
