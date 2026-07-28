import { SecretsManagerClient, GetSecretValueCommand } from "@aws-sdk/client-secrets-manager";
import { z } from "zod";
import { geminiParsedFoodLogSchema } from "./schemas.js";

const MODEL_NAME = "gemini-2.0-flash";
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL_NAME}:generateContent`;
const secretsClient = new SecretsManagerClient({});

const geminiApiSecretArn = requiredEnv("GEMINI_API_SECRET_ARN");

type ParsedFoodLog = z.infer<typeof geminiParsedFoodLogSchema>;

let cachedApiKey: string | null = null;

export class GeminiFoodLogService {
  async parseFoodLog(
    transcript: string,
    date: string,
    fallbackMeal: "Breakfast" | "Lunch" | "Dinner" | "Snack",
  ): Promise<ParsedFoodLog> {
    const apiKey = await loadGeminiApiKey();
    const prompt = buildPrompt(transcript, date, fallbackMeal);
    const response = await fetch(`${GEMINI_URL}?key=${encodeURIComponent(apiKey)}`, {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        contents: [
          {
            role: "user",
            parts: [{ text: prompt }],
          },
        ],
        generationConfig: {
          temperature: 0.1,
          responseMimeType: "application/json",
          responseSchema: {
            type: "OBJECT",
            properties: {
              entries: {
                type: "ARRAY",
                items: {
                  type: "OBJECT",
                  properties: {
                    foodName: { type: "STRING" },
                    brand: { type: "STRING" },
                    meal: { type: "STRING", enum: ["Breakfast", "Lunch", "Dinner", "Snack"] },
                    quantity: { type: "NUMBER" },
                    unit: { type: "STRING" },
                    date: { type: "STRING" },
                    notes: { type: "STRING" },
                  },
                  required: ["foodName"],
                },
              },
            },
            required: ["entries"],
          },
        },
      }),
    });

    if (!response.ok) {
      const responseText = await response.text();
      throw new Error(`Gemini request failed with status ${response.status}: ${responseText}`);
    }

    const payload = await response.json() as GeminiGenerateContentResponse;
    const text = payload.candidates?.[0]?.content?.parts
      ?.map((part) => part.text ?? "")
      .join("")
      .trim();

    if (!text) {
      throw new Error("Gemini returned an empty response");
    }

    return geminiParsedFoodLogSchema.parse(JSON.parse(text));
  }
}

async function loadGeminiApiKey(): Promise<string> {
  if (cachedApiKey) {
    return cachedApiKey;
  }

  const response = await secretsClient.send(
    new GetSecretValueCommand({
      SecretId: geminiApiSecretArn,
    }),
  );

  const secretString = response.SecretString?.trim();
  if (!secretString) {
    throw new Error("Gemini API secret is empty");
  }

  cachedApiKey = extractApiKey(secretString);
  return cachedApiKey;
}

function extractApiKey(secretString: string): string {
  if (!secretString.startsWith("{")) {
    return secretString;
  }

  const parsed = JSON.parse(secretString) as Record<string, unknown>;
  const apiKey = parsed.apiKey ?? parsed.key ?? parsed.GEMINI_API_KEY ?? parsed.geminiApiKey;
  if (typeof apiKey !== "string" || apiKey.trim().length === 0) {
    throw new Error("Gemini API secret JSON must contain an API key string");
  }
  return apiKey.trim();
}

function buildPrompt(
  transcript: string,
  date: string,
  fallbackMeal: string,
): string {
  return [
    "Parse the following food log transcript into structured food entries.",
    "Return JSON only.",
    `Default date: ${date}`,
    `Fallback meal: ${fallbackMeal}`,
    "For each entry, capture:",
    "- foodName: the item name",
    "- brand: optional brand if clearly stated",
    "- meal: Breakfast, Lunch, Dinner, or Snack if stated or strongly implied; otherwise use the fallback meal",
    "- quantity: numeric quantity if present",
    "- unit: serving unit if present",
    "- date: use the explicit date if stated, otherwise the default date",
    "- notes: optional short clarification only when needed",
    "Do not infer calories or nutrition.",
    "Do not invent foods that are not mentioned.",
    "Transcript:",
    transcript.trim(),
  ].join("\n");
}

function requiredEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing environment variable ${name}`);
  }
  return value;
}

interface GeminiGenerateContentResponse {
  candidates?: Array<{
    content?: {
      parts?: Array<{
        text?: string;
      }>;
    };
  }>;
}
