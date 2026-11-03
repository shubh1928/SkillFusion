import {
  useEffect,
  useRef,
  useState,
  useCallback,
} from "react";

import type { IMessage } from "@stomp/stompjs";
import NegotiationWebSocketService
    from "@/services/NegotiationWebSocketService";

import type AgreementVersion from
  "@/models/agreement/AgreementVersion";

import {
  getAgreementVersions,
} from "@/services/AgreementVersionService";

import AgreementVersionWorkflow from
  "./AgreementVersionWorkflow";

import {
  useParams,
} from "react-router";

import toast from "react-hot-toast";

import {
  MessageCircleMore,
} from "lucide-react";

import {
  getNegotiation,
  getMessages,
  requestSession,
  acceptSessionRequest,
} from "@/services/NegotiationService";

import type Negotiation
from "@/models/negotiation/Negotiation";

import type NegotiationMessage
from "@/models/negotiation/NegotiationMessage";

import UpdateDealDialog
from "./UpdateDealDialog";

import { Spinner }
from "@/components/ui/spinner";
import { Button } from "@/components/ui/button";

import useAuth from "@/auth/store";

function ConversationPage() {

    const {negotiationId,} = useParams();

    const auth = useAuth();

const currentUserId =
  auth.user?.id;

const [loading, setLoading] =
  useState(true);

const [negotiation, setNegotiation] =
  useState<Negotiation | null>(null);

const [messages, setMessages] =
  useState<NegotiationMessage[]>([]);

const [versions, setVersions] =
  useState<AgreementVersion[]>([]);

  const isRequester =
  currentUserId ===
  negotiation?.requesterId;

const isProvider =
  currentUserId ===
  negotiation?.providerId;

  const requiresInitialVersion =
  negotiation?.releaseModeSelectionRequired === true &&
  negotiation.exchangeMode !== "DIRECT_BARTER";

const initialVersionReady = versions.some(
  (version) =>
    version.status === "READY_FOR_ACTIVATION",
);

  const [message, setMessage] =
  useState("");

  const [updateDealOpen, setUpdateDealOpen] =
  useState(false);

  const messagesEndRef =
  useRef<HTMLDivElement | null>(null);

  const handleIncomingChatMessage = useCallback(

    (stompMessage: IMessage) => {

        try {

            const incomingMessage =
                JSON.parse(
                    stompMessage.body
                ) as NegotiationMessage;


            if (!incomingMessage.messageId) {

                console.warn(
                    "Negotiation WebSocket message has no messageId."
                );

                return;

            }


            setMessages((previous) => {

                const messageMap =
                    new Map<string, NegotiationMessage>();


                for (const existingMessage of previous) {

                    messageMap.set(
                        existingMessage.messageId,
                        existingMessage
                    );

                }


                if (
                    !messageMap.has(
                        incomingMessage.messageId
                    )
                ) {

                    messageMap.set(
                        incomingMessage.messageId,
                        incomingMessage
                    );

                }


                return Array.from(
                    messageMap.values()
                ).sort(

                    (a, b) =>

                        new Date(
                            a.createdAt
                        ).getTime()

                        -

                        new Date(
                            b.createdAt
                        ).getTime()

                );

            });

        } catch (error) {

            console.error(
                "Failed to process negotiation WebSocket message.",
                error
            );

        }

    },

    []

);

  const loadConversation = useCallback(async () => {

  if (!negotiationId) {

    return;

  }

  try {

    setLoading(true);

    const [
  negotiationResponse,
  messagesResponse,
  versionsResponse,
] = await Promise.all([
  getNegotiation(negotiationId),
  getMessages(negotiationId),
  getAgreementVersions(negotiationId),
]);

setNegotiation(negotiationResponse);
setVersions(versionsResponse);

    setMessages((previous) => {

    const messageMap =
        new Map<string, NegotiationMessage>();


    for (const existingMessage of previous) {

        messageMap.set(
            existingMessage.messageId,
            existingMessage
        );

    }


    for (const historyMessage of messagesResponse) {

        messageMap.set(
            historyMessage.messageId,
            historyMessage
        );

    }


    return Array.from(
        messageMap.values()
    ).sort(

        (a, b) =>

            new Date(
                a.createdAt
            ).getTime()

            -

            new Date(
                b.createdAt
            ).getTime()

    );

});

  } catch (error: any) {

    toast.error(

      error?.response?.data?.message ??

      "Failed to load conversation."

    );

  } finally {

    setLoading(false);

  }

}, [
  negotiationId,
]);




useEffect(() => {

    if (!negotiationId) {
        return;
    }


    let cancelled = false;


    const initializeConversation = async () => {

        /*
         * Subscribe first.
         *
         * This ensures the live channel is active
         * before we retrieve the existing history.
         */
        try {

            await NegotiationWebSocketService
                .subscribeToNegotiation(

                    negotiationId,

                    handleIncomingChatMessage

                );

        } catch (error) {

            /*
             * WebSocket failure should not prevent
             * the user from loading message history.
             */
            console.error(
                "Negotiation WebSocket connection failed.",
                error
            );

        }


        if (cancelled) {
            return;
        }


        await loadConversation();

    };


    initializeConversation();


    return () => {

        cancelled = true;

        NegotiationWebSocketService.disconnect();

    };

}, [
    negotiationId,
    handleIncomingChatMessage,
    loadConversation,
]);


const handleSendMessage = () => {

    if (
        !negotiationId ||
        message.trim() === ""
    ) {

        return;

    }


    try {

        NegotiationWebSocketService.sendMessage(

            negotiationId,

            message

        );


        setMessage("");

    } catch (error: any) {

        toast.error(

            error?.message ??

            "Unable to send message."

        );

    }

};


const handleRequestSession = async () => {

  if (!negotiationId) {

    return;

  }

  try {


await requestSession(
  negotiationId
);

toast.success(
  "Session request sent."
);

await loadConversation();

  } catch (error: any) {

    toast.error(

      error?.response?.data?.message ??

      "Unable to request session."

    );

  }

};



const handleAcceptSession = async () => {

  if (!negotiationId) {

    return;

  }

  try {

    await acceptSessionRequest(
      negotiationId
    );

    toast.success(
      "Session accepted successfully."
    );

    await loadConversation();

  } catch (error: any) {

    toast.error(

      error?.response?.data?.message ??

      "Unable to accept session."

    );

  }

};



useEffect(() => {

  messagesEndRef.current?.scrollIntoView({

    behavior: "smooth",

  });

}, [
  messages,
]);

  return (

    <div className="h-full">

      <div
        className="
        flex
        h-[calc(100vh-8rem)]
        overflow-hidden
        rounded-xl
        border
        "
      >

        {/* Left Chat Section */}

        <div
          className="
          flex
          flex-1
          flex-col
          border-r
          "
        >

          <div
            className="
            flex
            items-center
            gap-3
            border-b
            p-5
            "
          >

            <MessageCircleMore
              className="text-primary"
              size={26}
            />

            <div>

              <h2 className="text-xl font-semibold">

                Conversation

              </h2>

              <p className="text-sm text-muted-foreground">

                {loading
                    ? "Loading..."
                    : negotiation?.offerTitle}

              </p>

            </div>

          </div>

          {/* Messages */}

          <div
            className="
            flex-1
            overflow-y-auto
            p-6
            "
          >

{loading ? (

  <div className="flex justify-center py-10">

    <Spinner />

  </div>

) : messages.length === 0 ? (

  <div className="py-10 text-center text-muted-foreground">

    No messages yet.

  </div>

) : (

  <div className="space-y-5">

    {messages.map((message) => (

      <div
        key={message.messageId}
        className={
          message.messageType === "SYSTEM"
            ? "flex justify-center"
            : message.senderId === negotiation?.requesterId
            ? "flex justify-start"
            : "flex justify-end"
        }
      >

        {message.messageType === "SYSTEM" ? (

          <div
            className="
            rounded-full
            bg-muted
            px-4
            py-2
            text-xs
            text-muted-foreground
            "
          >

            {message.message}

          </div>

        ) : (

          <div
            className="
            max-w-[70%]
            rounded-2xl
            border
            bg-card
            p-4
            shadow-sm
            "
          >

            <div className="mb-2 text-xs font-semibold text-primary">

              {message.senderName}

            </div>

            <p className="whitespace-pre-wrap">

              {message.message}

            </p>

            <div className="mt-2 text-right text-[11px] text-muted-foreground">

              {new Date(
                message.createdAt
              ).toLocaleString()}

            </div>

          </div>

        )}

      </div>

    ))}

    <div ref={messagesEndRef} />

  </div>

)}

          </div>

          {/* Message Box */}

<div
  className="
  border-t
  p-4
  "
>

  <div className="flex gap-3">

    <textarea
      value={message}
      onChange={(e) =>
        setMessage(
          e.target.value
        )
      }

        onKeyDown={(event) => {

        if (

            event.key === "Enter" &&

            !event.shiftKey

        ) {

            event.preventDefault();

            handleSendMessage();

        }

        }}

      rows={2}
      placeholder="Type your message..."
      className="
        flex-1
        resize-none
        rounded-lg
        border
        bg-background
        p-3
        outline-none
      "

      
    />

    <button
      onClick={handleSendMessage}
      className="
        rounded-lg
        bg-primary
        px-6
        text-primary-foreground
        transition-colors
        hover:opacity-90
      "
    >

      Send

    </button>

  </div>

</div>

        </div>

        {/* Right Deal Section */}

        <div
          className="
          hidden
          w-[360px]
          flex-col
          lg:flex
          "
        >

          <div
            className="
            border-b
            p-5
            "
          >

            <h2 className="text-lg font-semibold">

              Current Deal

            </h2>

          </div>

          <div
  className="
  flex-1
  space-y-6
  overflow-y-auto
  p-5
  "
>

  <div>

    <p className="text-sm text-muted-foreground">

      Offering

    </p>

    <h3 className="mt-1 font-semibold">

      {negotiation?.offerTitle}

    </h3>

    <p className="mt-2 text-sm text-muted-foreground">

      {negotiation?.offerDescription}

    </p>

  </div>

  <div>

    <p className="text-sm text-muted-foreground">

      Looking For

    </p>

    <h3 className="mt-1 font-semibold">

      {negotiation?.requestTitle}

    </h3>

    <p className="mt-2 text-sm text-muted-foreground">

      {negotiation?.requestDescription}

    </p>

  </div>

  <div className="grid grid-cols-2 gap-4">

    <div>

      <p className="text-sm text-muted-foreground">

        Exchange

      </p>

      <p className="font-medium">

        {negotiation?.exchangeMode.replaceAll(
          "_",
          " "
        )}

      </p>

    </div>

    <div>

      <p className="text-sm text-muted-foreground">

        Hours

      </p>

      <p className="font-medium">

        {negotiation?.estimatedHours}

      </p>

    </div>

  </div>

  {negotiation?.tokenAmount != null && (

    <div>

      <p className="text-sm text-muted-foreground">

        Tokens

      </p>

      <p className="font-medium">

        {negotiation.tokenAmount}

      </p>

    </div>

    

  )}

  <AgreementVersionWorkflow
  negotiation={negotiation}
  versions={versions}
  currentUserId={currentUserId}
  onRefresh={loadConversation}
/>

</div>

<div
  className="
  border-t
  p-5
  "
>

<div className="space-y-3">

  <Button
    className="w-full"
    onClick={() =>
      setUpdateDealOpen(true)
    }
  >
    Update Deal
  </Button>


  {isProvider &&
  negotiation?.status === "ACTIVE" &&
  requiresInitialVersion &&
  !initialVersionReady && (
    <p className="text-xs text-muted-foreground">
      Complete the release-mode selection and obtain
      both participants' agreement acceptance before
      requesting the session.
    </p>
  )}

{isProvider &&
  negotiation?.status === "ACTIVE" &&
  (!requiresInitialVersion || initialVersionReady) && (
    <Button
      variant="outline"
      className="w-full"
      onClick={handleRequestSession}
    >
      Request Session
    </Button>
  )}

{isRequester &&
  negotiation?.status ===
    "SESSION_REQUESTED" && (
      <Button
        variant="outline"
        className="w-full"
        onClick={handleAcceptSession}
      >
        Accept Session
      </Button>
)}

</div>

</div>

        </div>

      </div>

<UpdateDealDialog
  open={updateDealOpen}
  onOpenChange={setUpdateDealOpen}
  negotiation={negotiation}
  onSuccess={loadConversation}
/>

    </div>

  );

}

export default ConversationPage;