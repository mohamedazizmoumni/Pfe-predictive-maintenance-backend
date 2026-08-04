-- Discloses whether a stored prediction came from the trained model or the
-- rule-based fallback (MLServiceClient.createFallbackPrediction, tagged
-- modelVersion "fallback-*"). Previously ml_prediction_available was hardcoded
-- true regardless of which one produced the row, so fallback data was
-- indistinguishable from real model output.
ALTER TABLE prediction_history ADD COLUMN model_version VARCHAR(50);
