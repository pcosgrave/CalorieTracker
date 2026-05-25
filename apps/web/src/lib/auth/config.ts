export type CognitoConfig = {
  region: string;
  domain: string;
  userPoolId: string;
  webClientId: string;
  androidClientId: string;
  apiBaseUrl: string;
  webRedirectUri: string;
  webLogoutUri: string;
};

const defaultConfig: CognitoConfig = {
  region: "us-east-1",
  domain: "cosgravelabs-bitewise-dev",
  userPoolId: "us-east-1_WYQwdC4oO",
  webClientId: "vq2ov6pm1e7oub9h8r2g5924s",
  androidClientId: "bb27drot68rek496i4tmrn2ik",
  apiBaseUrl: "https://84jfkxkrd6.execute-api.us-east-1.amazonaws.com/dev",
  webRedirectUri: "http://localhost:3000/auth/callback",
  webLogoutUri: "http://localhost:3000/",
};

export function getCognitoConfig(): CognitoConfig {
  return {
    region: process.env.NEXT_PUBLIC_AWS_REGION || defaultConfig.region,
    domain: process.env.NEXT_PUBLIC_COGNITO_DOMAIN || defaultConfig.domain,
    userPoolId: process.env.NEXT_PUBLIC_COGNITO_USER_POOL_ID || defaultConfig.userPoolId,
    webClientId: process.env.NEXT_PUBLIC_COGNITO_WEB_CLIENT_ID || defaultConfig.webClientId,
    androidClientId: process.env.NEXT_PUBLIC_COGNITO_ANDROID_CLIENT_ID || defaultConfig.androidClientId,
    apiBaseUrl: process.env.NEXT_PUBLIC_SYNC_API_BASE_URL || defaultConfig.apiBaseUrl,
    webRedirectUri: process.env.NEXT_PUBLIC_COGNITO_WEB_REDIRECT_URI || defaultConfig.webRedirectUri,
    webLogoutUri: process.env.NEXT_PUBLIC_COGNITO_WEB_LOGOUT_URI || defaultConfig.webLogoutUri,
  };
}

export function cognitoIssuer(config = getCognitoConfig()): string {
  return `https://cognito-idp.${config.region}.amazonaws.com/${config.userPoolId}`;
}

export function cognitoHostedUiBase(config = getCognitoConfig()): string {
  return `https://${config.domain}.auth.${config.region}.amazoncognito.com`;
}
