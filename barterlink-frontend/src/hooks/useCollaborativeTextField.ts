
import { useCallback, useEffect, useMemo, useState } from "react";
import type { AgreementYjsDocumentService } from "@/services/AgreementYjsDocumentService";

export type AgreementTextField =
  | "agreedTerms"
  | "agreedDeadline"
  | "changeSummary";

export type AgreementMilestoneTextField =
  | "milestoneNumber"
  | "title"
  | "deliverable"
  | "deadline"
  | "tokenAllocation";

interface UseCollaborativeTextFieldOptions {
  manager: AgreementYjsDocumentService | null;
  field: AgreementTextField | AgreementMilestoneTextField;
  milestoneId?: string;
  fallbackValue?: string;
}

const LOCAL_INPUT_ORIGIN = "local-agreement-input";

export function useCollaborativeTextField({
  manager,
  field,
  milestoneId,
  fallbackValue = "",
}: UseCollaborativeTextFieldOptions) {
  const [value, setValue] = useState(fallbackValue);

  const sharedText = useMemo(() => {
    if (!manager) {
      return null;
    }

    if (milestoneId) {
      return manager.getMilestoneTextField(
        milestoneId,
        field as AgreementMilestoneTextField,
      );
    }

    return manager.getTextField(
      field as AgreementTextField,
    );
  }, [manager, field, milestoneId]);

  /*
   * Observe changes from both local and remote participants.
   */
  useEffect(() => {
    if (!sharedText) {
      setValue(fallbackValue);
      return;
    }

    const synchronizeValue = () => {
      setValue(sharedText.toString());
    };

    sharedText.observe(synchronizeValue);

    synchronizeValue();

    return () => {
      sharedText.unobserve(synchronizeValue);
    };
  }, [sharedText, fallbackValue]);

  /*
   * Apply only the changed portion of the text.
   *
   * We deliberately avoid clearing and replacing the entire
   * Y.Text because that would create unnecessary conflicts
   * when both participants edit the same field.
   */
  const onChange = useCallback(
    (nextValue: string) => {
      if (!sharedText || !manager) {
        setValue(nextValue);
        return;
      }

      const currentValue = sharedText.toString();

      let prefixLength = 0;

      while (
        prefixLength < currentValue.length &&
        prefixLength < nextValue.length &&
        currentValue[prefixLength] === nextValue[prefixLength]
      ) {
        prefixLength++;
      }

      let suffixLength = 0;

      while (
        suffixLength <
          currentValue.length - prefixLength &&
        suffixLength <
          nextValue.length - prefixLength &&
        currentValue[
          currentValue.length - 1 - suffixLength
        ] ===
          nextValue[
            nextValue.length - 1 - suffixLength
          ]
      ) {
        suffixLength++;
      }

      const deleteLength =
        currentValue.length -
        prefixLength -
        suffixLength;

      const insertedText = nextValue.slice(
        prefixLength,
        nextValue.length - suffixLength,
      );

      manager.doc.transact(() => {
        if (deleteLength > 0) {
          sharedText.delete(
            prefixLength,
            deleteLength,
          );
        }

        if (insertedText.length > 0) {
          sharedText.insert(
            prefixLength,
            insertedText,
          );
        }
      }, LOCAL_INPUT_ORIGIN);

      setValue(nextValue);
    },
    [manager, sharedText],
  );

  return {
    value,
    onChange,
    isCollaborative: !!sharedText,
  };
}
