
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Clock } from "lucide-react";
import type { FetchBaseQueryError } from "@reduxjs/toolkit/query";
import { Badge } from "../../../components/ui/badge";
import { Form } from "../../../components/ui/form";
import SignatureForm from "../../../components/clinical-forms/SignatureForm";
import PortalFormFieldRenderer from "../../../components/clinical-forms/PortalFormFieldRenderer";
import Toast from "@/components/shared/Toast";
import { useForm } from "react-hook-form";
import { cn } from "@/lib/utils";
import { useGetPortalMeQuery } from "@/store/api/portalApi";
import {
  useGetPortalFormAssignmentByIdQuery,
  useGetPortalFormResponsesQuery,
  useGetPortalFormSignatureQuery,
  useSavePortalFormResponseMutation,
  useSavePortalFormSignatureMutation,
  useSubmitPortalFormAssignmentMutation,
} from "@/store/api/portalFormsApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  buildPortalFormFieldValues,
  calculatePortalFormProgress,
  getPortalFormDetailTitle,
  getPortalFormStatusLabel,
  isPortalFormReadOnly,
  isPortalFormSignatureField,
  mapPortalFormStatus,
} from "@/utils/portalFormDisplay";
import {
  getPortalAssignmentFieldId,
  getPortalSavableFormFields,
  normalizePortalSignatureData,
} from "@/utils/portalFormSubmission";
import type { PortalClinicalFormValues } from "@/components/clinical-forms/PortalFormFieldRenderer";
import { getPortalFormPlaceholdersFromAssignment } from "@/utils/portalFormPlaceholders";

const ClinicalFormDetailContent = () => {
  const navigate = useNavigate();
  const { formId } = useParams<{ formId: string }>();
  const assignmentId = Number(formId);
  const hasValidAssignmentId = Number.isFinite(assignmentId) && assignmentId > 0;

  const [fieldValues, setFieldValues] = useState<Record<number, string>>({});
  const [signedAt, setSignedAt] = useState<Date | undefined>(undefined);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSavingDraft, setIsSavingDraft] = useState(false);
  const [hasHydratedResponses, setHasHydratedResponses] = useState(false);
  const [draftNotice, setDraftNotice] = useState<{ error: boolean; message: string } | null>(null);
  const writeInFlight = useRef(false);
  const isBusy = isSubmitting || isSavingDraft;
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("error");

  const { data: portalMe } = useGetPortalMeQuery();

  const {
    data: assignmentDetail,
    isLoading: isLoadingDetail,
    isFetching: isFetchingDetail,
    isError: isDetailError,
    error: detailError,
  } = useGetPortalFormAssignmentByIdQuery(assignmentId, {
    skip: !hasValidAssignmentId,
  });

  const {
    data: savedResponses = [],
    isLoading: isLoadingResponses,
    isFetching: isFetchingResponses,
    isError: isResponsesError,
    error: responsesError,
  } = useGetPortalFormResponsesQuery(assignmentId, {
    skip: !hasValidAssignmentId,
  });

  const {
    data: savedSignature,
    isLoading: isLoadingSignature,
    isFetching: isFetchingSignature,
    isError: isSignatureError,
    error: signatureError,
  } = useGetPortalFormSignatureQuery(assignmentId, {
    skip: !hasValidAssignmentId,
  });

  const [savePortalFormResponse] = useSavePortalFormResponseMutation();
  const [savePortalFormSignature] = useSavePortalFormSignatureMutation();
  const [submitPortalFormAssignment] = useSubmitPortalFormAssignmentMutation();

  const form = useForm<PortalClinicalFormValues>({
    defaultValues: {
      clientName: "",
      date: new Date().toISOString().slice(0, 10),
      signature: "",
    },
  });

  const isReadOnly = isPortalFormReadOnly(assignmentDetail?.status) ||
    !assignmentDetail || !hasHydratedResponses;
  const formStatus = mapPortalFormStatus(assignmentDetail?.status);
  const requiresSignature =
    Boolean(assignmentDetail?.requiresSignature) ||
    (assignmentDetail?.fields ?? []).some(isPortalFormSignatureField);

  const formFields = useMemo(
    () => assignmentDetail?.fields ?? [],
    [assignmentDetail?.fields],
  );

  const placeholders = useMemo(
    () => getPortalFormPlaceholdersFromAssignment(assignmentDetail),
    [assignmentDetail],
  );

  const savableFields = useMemo(
    () => getPortalSavableFormFields(formFields),
    [formFields],
  );

  const isInitialLoading =
    !hasValidAssignmentId ||
    (!hasHydratedResponses && !isDetailError && !isResponsesError) ||
    ((isLoadingDetail ||
      isFetchingDetail ||
      isLoadingResponses ||
      isFetchingResponses ||
      isLoadingSignature ||
      isFetchingSignature) &&
      (!assignmentDetail || !hasHydratedResponses));

  useEffect(() => {
    if (!portalMe?.fullName) return;
    form.setValue("clientName", portalMe.fullName);
  }, [form, portalMe?.fullName]);

  useEffect(() => {
    // Hydrate once per assignment. Mutation refetches must not replace edits,
    // especially after a partially successful draft save.
    if (hasHydratedResponses || isLoadingResponses || isFetchingResponses ||
        isResponsesError || !assignmentDetail) return;
    const next = buildPortalFormFieldValues(savedResponses);
    const clientFullName = placeholders.CLIENT_FULL_NAME?.trim() ?? "";
    if (clientFullName) {
      for (const field of formFields) {
        const label = field.label?.trim().toLowerCase() ?? "";
        if (["client full name", "full name", "client name"].includes(label) &&
            !next[field.id]?.trim()) next[field.id] = clientFullName;
      }
    }
    setFieldValues(next);
    setHasHydratedResponses(true);
  }, [assignmentDetail, formFields, hasHydratedResponses, isFetchingResponses,
      isLoadingResponses, isResponsesError, placeholders.CLIENT_FULL_NAME, savedResponses]);

  useEffect(() => {
    if (!savedSignature?.signatureData) return;
    form.setValue("signature", savedSignature.signatureData);
    if (savedSignature.signedAt) {
      const parsed = new Date(savedSignature.signedAt);
      if (!Number.isNaN(parsed.getTime())) {
        setSignedAt(parsed);
      }
    }
  }, [form, savedSignature]);

  useEffect(() => {
    const loadError = detailError ?? responsesError ?? signatureError;
    const signatureStatus =
      signatureError && typeof signatureError === "object"
        ? (signatureError as FetchBaseQueryError).status
        : undefined;

    if (!isDetailError && !isResponsesError && !isSignatureError) return;
    if (isSignatureError && signatureStatus === 404) return;

    setToastType("error");
    setToastMessage(getApiErrorMessage(loadError));
  }, [
    detailError,
    isDetailError,
    isResponsesError,
    isSignatureError,
    responsesError,
    signatureError,
  ]);

  const signatureValue = form.watch("signature");
  const progress = useMemo(
    () =>
      calculatePortalFormProgress(
        formFields,
        fieldValues,
        Boolean(signatureValue?.trim()),
        requiresSignature,
      ),
    [fieldValues, formFields, requiresSignature, signatureValue],
  );

  const handleFieldValueChange = (fieldId: number, value: string) => {
    setDraftNotice(null);
    setFieldValues((current) => ({
      ...current,
      [fieldId]: value,
    }));
  };

  const validateBeforeSubmit = () => {
    const missingRequiredField = savableFields.find(
      (field) => field.isRequired && !fieldValues[field.id]?.trim(),
    );

    if (missingRequiredField) {
      setToastType("error");
      setToastMessage(
        `Please complete "${missingRequiredField.label}" before submitting.`,
      );
      return false;
    }

    if (requiresSignature && !signatureValue?.trim()) {
      setToastType("error");
      setToastMessage("Please add your signature before submitting.");
      return false;
    }

    return true;
  };

  const saveAnswers = async () => {
    // Include empty values so clearing a saved answer persists on refresh.
    for (const field of savableFields) {
      await savePortalFormResponse({
        assignmentId,
        assignmentFieldId: getPortalAssignmentFieldId(field),
        value: fieldValues[field.id] ?? "",
      }).unwrap();
    }
  };

  const onSaveDraft = async () => {
    if (!hasValidAssignmentId || isReadOnly || writeInFlight.current) return;
    writeInFlight.current = true;
    setIsSavingDraft(true);
    setDraftNotice(null);
    try {
      await saveAnswers();
      setDraftNotice({ error: false, message: "Draft saved. Your answers will be here when you return." });
    } catch {
      setDraftNotice({ error: true, message: "Draft could not be fully saved. Your edits are still here. Please retry." });
    } finally {
      writeInFlight.current = false;
      setIsSavingDraft(false);
    }
  };

  const onSubmit = async (data: PortalClinicalFormValues) => {
    if (!hasValidAssignmentId || isReadOnly || writeInFlight.current) return;
    if (!validateBeforeSubmit()) return;

    writeInFlight.current = true;
    setIsSubmitting(true);
    setDraftNotice(null);

    try {
      await saveAnswers();

      if (requiresSignature) {
        const signatureData = await normalizePortalSignatureData(data.signature);
        if (!signatureData) {
          setToastType("error");
          setToastMessage("Please add your signature before submitting.");
          return;
        }

        await savePortalFormSignature({
          assignmentId,
          signatureData,
        }).unwrap();
      }

      await submitPortalFormAssignment(assignmentId).unwrap();
      setSignedAt(new Date());
      setToastType("success");
      setToastMessage("Form submitted successfully.");
    } catch (submitError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(submitError));
    } finally {
      writeInFlight.current = false;
      setIsSubmitting(false);
    }
  };

  if (!hasValidAssignmentId) {
    return (
      <div className="min-h-screen bg-(--bg-primary-light)">
        <div className="sticky top-0 z-20 border-b border-transparent bg-(--bg-primary-light) px-4 pt-8 pb-4">
          <div className="mx-auto max-w-4xl">
            <button
              type="button"
              onClick={() => navigate("/user/clinical-forms")}
              className="flex cursor-pointer items-center gap-2 text-gray-600 transition-colors hover:text-gray-900"
            >
              <ArrowLeft size={20} />
              <span className="text-sm font-medium">Back to Forms</span>
            </button>
          </div>
        </div>
        <div className="px-4 pb-8">
          <div className="mx-auto max-w-4xl text-sm text-(--text-neutral-600)">
            Invalid form assignment.
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-(--bg-primary-light)">
      <div className="sticky top-0 z-20 border-b border-transparent bg-(--bg-primary-light) px-4 pt-8 pb-4">
        <div className="mx-auto max-w-4xl">
          <button
            type="button"
            onClick={() => navigate("/user/clinical-forms")}
            className="flex cursor-pointer items-center gap-2 text-gray-600 transition-colors hover:text-gray-900"
          >
            <ArrowLeft size={20} />
            <span className="text-sm font-medium">Back to Forms</span>
          </button>
        </div>
      </div>

      <div className="px-4 pb-8">
        <div className="mx-auto max-w-4xl min-w-0">
        <div className="bg-white rounded-xl shadow-sm p-8 min-w-0 overflow-hidden">
          {isInitialLoading ? (
            <div className="flex items-center justify-center gap-2 py-16 text-sm text-(--text-neutral-600)">
              <ContentLoader variant="inline" size="md" />
              <span>Loading form...</span>
            </div>
          ) : (
            <>
              <div className="mb-6 min-w-0">
                <div className="flex items-start justify-between mb-4 gap-4 min-w-0">
                  <h1 className="min-w-0 flex-1 text-2xl font-semibold text-(--text-primary-dark) break-words [overflow-wrap:anywhere]">
                    {assignmentDetail
                      ? getPortalFormDetailTitle(assignmentDetail)
                      : "Clinical Form"}
                  </h1>
                  <Badge
                    variant="outline"
                    className={cn(
                      "text-xs font-normal flex items-center gap-1.5 border-none shrink-0",
                      formStatus === "completed"
                        ? "bg-(--status-completed-light) text-(--status-completed-dark)"
                        : "bg-gray-100 text-gray-600",
                    )}
                  >
                    <Clock size={14} />
                    {getPortalFormStatusLabel(formStatus)}
                  </Badge>
                </div>

                {assignmentDetail?.templateDescription ? (
                  <p className="mb-4 min-w-0 text-sm text-(--text-neutral-600) break-words whitespace-pre-wrap [overflow-wrap:anywhere]">
                    {assignmentDetail.templateDescription}
                  </p>
                ) : null}

                {assignmentDetail?.templateInstructions ? (
                  <p className="mb-4 min-w-0 text-sm text-(--text-primary-dark) break-words whitespace-pre-wrap [overflow-wrap:anywhere]">
                    {assignmentDetail.templateInstructions}
                  </p>
                ) : null}

                <div className="mb-6">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm text-(--text-neutral-600)">
                      Progress: {progress}%
                    </span>
                  </div>
                  <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-[var(--btn-default-bg)] rounded-full transition-all"
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                </div>
              </div>

              <div className="space-y-6 mb-8">
                {formFields.length > 0 ? (
                  formFields.map((field) => (
                    <PortalFormFieldRenderer
                      key={field.id}
                      field={field}
                      value={fieldValues[field.id] ?? ""}
                      disabled={isReadOnly || isBusy}
                      placeholders={placeholders}
                      onValueChange={handleFieldValueChange}
                    />
                  ))
                ) : (
                  <p className="text-sm text-(--text-neutral-600)">
                    No form fields are available for this assignment.
                  </p>
                )}
              </div>

              {!isReadOnly && savableFields.length > 0 ? (
                <div className="mb-6 space-y-2">
                  <button
                    type="button"
                    disabled={isBusy}
                    onClick={onSaveDraft}
                    className="inline-flex h-11 items-center justify-center rounded-full border border-gray-300 px-6 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {isSavingDraft ? "Saving draft..." : "Save Draft"}
                  </button>
                  <p className="text-sm text-(--text-neutral-600)">
                    Save your answers now. Signatures are saved only when you submit.
                  </p>
                  {draftNotice ? (
                    <p role={draftNotice.error ? "alert" : "status"} className="text-sm">
                      {draftNotice.message}
                    </p>
                  ) : null}
                </div>
              ) : null}

              {requiresSignature ? (
                <section>
                  <Form {...form}>
                    <SignatureForm
                      control={form.control}
                      form={form}
                      onSubmit={onSubmit}
                      isReadOnly={isReadOnly}
                      isSubmitting={isBusy}
                      signedAt={signedAt}
                    />
                  </Form>
                </section>
              ) : !isReadOnly ? (
                <div className="flex justify-end">
                  <button
                    type="button"
                    disabled={isBusy}
                    onClick={form.handleSubmit(onSubmit)}
                    className="inline-flex h-11 items-center justify-center rounded-full bg-(--bg-primary-dark) px-6 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {isSubmitting ? (
                      <span className="inline-flex items-center gap-2">
                        <ContentLoader variant="inline" size="sm" />
                        Submitting...
                      </span>
                    ) : (
                      "Submit Form"
                    )}
                  </button>
                </div>
              ) : null}
            </>
          )}
        </div>
        </div>
      </div>

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </div>
  );
};

const ClinicalFormDetail = () => {
  const { formId } = useParams<{ formId: string }>();
  return <ClinicalFormDetailContent key={formId} />;
};

export default ClinicalFormDetail;
