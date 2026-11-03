import type { RefObject } from "react";

import type NegotiationMessage
    from "@/models/negotiation/NegotiationMessage";

import NegotiationMessageType
    from "@/models/negotiation/NegotiationMessageType";


interface ChatViewProps {
    messages: NegotiationMessage[];
    currentUserId?: string;

    message: string;

    onMessageChange: (value: string) => void;

    onSendMessage: () => void;

    messagesEndRef: RefObject<HTMLDivElement | null>;
}


export default function ChatView({

    messages,

    currentUserId,

    message,

    onMessageChange,

    onSendMessage,

    messagesEndRef,

}: ChatViewProps) {

    return (

             <div className="flex h-full flex-col rounded-xl border">

        <div className="border-b p-5">

          <h2 className="text-xl font-bold">

            Conversation

          </h2>

        </div>

        <div
              className="
                  flex-1
                  space-y-3
                  overflow-y-auto
                  rounded-lg
                  border
                  p-4
                  bg-muted/20
              "
          >

              {messages.map((message) => {

                  /*
                  * System messages
                  */
                  if (
                      message.messageType ===
                      NegotiationMessageType.SYSTEM
                  ) {

                      return (

                          <div
                              key={message.messageId}
                              className="my-4 flex items-center gap-3"
                          >

                              <div className="h-px flex-1 bg-border" />

                              <span
                                  className="
                                      text-xs
                                      font-medium
                                      uppercase
                                      tracking-wide
                                      text-muted-foreground
                                  "
                              >

                                  {message.message}

                              </span>

                              <div className="h-px flex-1 bg-border" />

                          </div>

                      );

                  }

                  const isMine =
                    message.senderId === currentUserId;

                  return (

                      <div
                          key={message.messageId}
                          className={`flex ${
                              isMine
                                  ? "justify-end"
                                  : "justify-start"
                          }`}
                      >

                          <div
                              className={`
                                  max-w-[75%]
                                  rounded-xl
                                  p-3
                                  shadow-sm

                                  ${
                                      isMine
                                          ? "bg-blue-600 text-white"
                                          : "border bg-card"
                                  }
                              `}
                          >

                              {!isMine && (

                                  <p className="mb-1 text-xs font-semibold">

                                      {message.senderName}

                                  </p>

                              )}

                              <p className="whitespace-pre-wrap">

                                  {message.message}

                              </p>

                              <p
                                  className={`
                                      mt-2
                                      text-right
                                      text-xs

                                      ${
                                          isMine
                                              ? "text-blue-100"
                                              : "text-muted-foreground"
                                      }
                                  `}
                              >

                                  {new Date(
                                      message.createdAt
                                  ).toLocaleTimeString([], {

                                      hour: "2-digit",

                                      minute: "2-digit",

                                  })}

                              </p>

                          </div>

                      </div>

                  );

              })}

          <div ref={messagesEndRef} />

        </div>

        <div className="border-t p-4">

          <div className="flex gap-3">

            <input
                className="flex-1 rounded-lg border p-3"
                placeholder="Type a message..."

                value={message}

                onChange={(event) =>
                        onMessageChange(event.target.value)
                    }

                onKeyDown={(event) => {

                    if (event.key === "Enter") {

                        onSendMessage();

                    }

                }}
            />

            <button
                className="
                    rounded-lg
                    bg-blue-600
                    px-6
                    py-3
                    text-white
                "
                disabled={!message.trim()}
                onClick={onSendMessage}
            >

              Send

            </button>

          </div>

        </div>

        </div>

    );

}

