import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogFooter,
} from "@/components/ui/dialog";

import { Button } from "@/components/ui/button";

import { useState } from "react";

import { Star } from "lucide-react";

import { Textarea } from "@/components/ui/textarea";

import toast from "react-hot-toast";

import {
    submitRating,
} from "@/services/RatingService";

interface SubmitRatingDialogProps {

    open: boolean;

    onOpenChange: (open: boolean) => void;

    sessionId: string;

    ratedUserName: string;

    onSubmitted: () => void;

}

function SubmitRatingDialog({

    open,

    onOpenChange,

    sessionId,

    ratedUserName,

    onSubmitted,

}: SubmitRatingDialogProps) {

    const [stars, setStars] = useState(0);

    const [review, setReview] = useState("");

    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async () => {

    if (stars === 0) {
        return;
    }

    try {

        setSubmitting(true);

        await submitRating({

            sessionId,

            stars,

            review,

        });

        toast.success(
            "Rating submitted successfully."
        );

        setStars(0);

        setReview("");

        onSubmitted();

        onOpenChange(false);

    } catch (error: any) {

        toast.error(

            error?.response?.data?.message ??

            "Unable to submit rating."

        );

    } finally {

        setSubmitting(false);

    }

};

    return (

        <Dialog
            open={open}
            onOpenChange={onOpenChange}
        >

            <DialogContent>

                <DialogHeader>

                    <DialogTitle>

                        ⭐ Rate Your Experience

                    </DialogTitle>

                    <DialogDescription>

                        Share your experience with{" "}
                        <strong>{ratedUserName}</strong>.

                    </DialogDescription>

                </DialogHeader>

                <div className="space-y-6">

                    <div className="space-y-2 text-center">

                        <p className="text-sm text-muted-foreground">

                            Select a rating

                        </p>

                        <div className="flex justify-center gap-2">

                            {[1, 2, 3, 4, 5].map((value) => (

                                <Button
                                    key={value}
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    onClick={() => setStars(value)}
                                >

                                    <Star
                                        className={
                                            value <= stars
                                                ? "fill-yellow-400 text-yellow-400"
                                                : "text-muted-foreground"
                                        }
                                    />

                                </Button>

                            ))}

                        </div>

                    </div>

                    <div className="space-y-2">

                        <label className="text-sm font-medium">

                            Review (optional)

                        </label>

                        <Textarea
                            value={review}
                            onChange={(event) =>
                                setReview(event.target.value)
                            }
                            placeholder="Share your experience..."
                        />

                    </div>

                </div>

                <DialogFooter>

                <Button
                    variant="outline"
                    onClick={() => onOpenChange(false)}
                >
                    Cancel
                </Button>

                <Button
    onClick={handleSubmit}
    disabled={
        stars === 0 ||
        submitting
    }
>

    {submitting
        ? "Submitting..."
        : "Submit Rating"}

</Button>

            </DialogFooter>

            </DialogContent>

        </Dialog>

    );

}

export default SubmitRatingDialog;