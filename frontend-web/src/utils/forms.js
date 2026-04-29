export function emptyToNull(value) {
  if (value == null) {
    return null;
  }

  const normalized = String(value).trim();
  return normalized ? normalized : null;
}

export function toIsoString(value) {
  if (!value) {
    return null;
  }
  return new Date(value).toISOString();
}

export function splitCsv(value) {
  return String(value || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

export function parseMedicationText(value) {
  return String(value || "")
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [name, dosage, frequency, remark] = line.split("|").map((item) => item.trim());

      if (!name || !dosage || !frequency) {
        throw new Error(`用药格式不完整：${line}`);
      }

      return {
        name,
        dosage,
        frequency,
        remark: remark || null,
      };
    });
}

export function mapProfileToForm(profile, fallbackName = "") {
  return {
    name: profile?.name || fallbackName,
    gender: profile?.gender || "MALE",
    birthday: profile?.birthday || "",
    heightCm: profile?.heightCm ?? "",
    targetUricAcid: profile?.targetUricAcid ?? "",
    allergies: (profile?.allergies || []).join(", "),
    comorbidities: (profile?.comorbidities || []).join(", "),
    emergencyContact: profile?.emergencyContact || "",
  };
}

export function mapMedicationToText(plan) {
  return {
    lines: (plan?.currentMedications || [])
      .map((item) => [item.name, item.dosage, item.frequency, item.remark || ""].join(" | "))
      .join("\n"),
    followUpNote: plan?.followUpNote || "",
  };
}
