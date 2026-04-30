package com.example.videochat.util;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

public class JwtUtils {

  public static Long getUserIdFromToken(String token) throws JSONException {
    if (token == null || token.isEmpty()) {
      return null;
    }

      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        return null;
      }

      String payload = parts[1];

      int mod = payload.length() % 4;
      if (mod > 0) {
        payload += "====".substring(mod);
      }

      byte[] decodedBytes = Base64.decode(payload, Base64.URL_SAFE);
      String jsonPayload = new String(decodedBytes, StandardCharsets.UTF_8);

      JSONObject jsonObject = new JSONObject(jsonPayload);

      if (jsonObject.has("userId")) {
        return jsonObject.getLong("userId");
      } else {
        return null;
      }
  }
}
