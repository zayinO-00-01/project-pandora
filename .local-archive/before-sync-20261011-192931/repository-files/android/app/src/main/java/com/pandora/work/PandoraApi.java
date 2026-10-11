package com.pandora.work;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Small dependency-free client matching backend/openapi.yaml v0. */
public final class PandoraApi {
    public interface Callback<T> { void success(T value); void failure(int status, String message); }
    public static final class Session { public final String token, role, displayName; Session(String t, String r, String n) { token=t; role=r; displayName=n; } }
    public static final class WorkLog { public final int id, userId; public final String logDate, content, status; WorkLog(JSONObject o) { id=o.optInt("id"); userId=o.optInt("userId"); logDate=o.optString("logDate"); content=o.optString("content"); status=o.optString("status"); } }
    private final String baseUrl; private final Handler main = new Handler(Looper.getMainLooper()); private volatile String token;
    public PandoraApi() { baseUrl = BuildConfig.API_BASE_URL.replaceAll("/$", ""); }
    public boolean isConfigured() { return !baseUrl.contains("<domain>"); }
    public void login(String username, String password, Callback<Session> callback) { request("POST", ApiContract.LOGIN, null, json("username", username, "password", password), body -> { JSONObject o=new JSONObject(body); token=o.optString("token"); return new Session(token, o.optString("role"), o.optString("displayName")); }, callback); }
    public void integrity(Callback<JSONObject> callback) { request("GET", ApiContract.LOG_INTEGRITY, token, null, JSONObject::new, callback); }
    public void listLogs(String date, Integer userId, Callback<List<WorkLog>> callback) { StringBuilder q=new StringBuilder(); if (date != null) q.append("?date=").append(encode(date)); if (userId != null) q.append(q.length()==0?"?":"&").append("userId=").append(userId); request("GET", ApiContract.LOGS+q, token, null, body -> { JSONArray a=new JSONArray(body); List<WorkLog> result=new ArrayList<>(); for(int i=0;i<a.length();i++) result.add(new WorkLog(a.getJSONObject(i))); return result; }, callback); }
    public void createLog(String logDate, String content, Callback<WorkLog> callback) { request("POST", ApiContract.LOGS, token, json("logDate", logDate, "content", content), body -> new WorkLog(new JSONObject(body)), callback); }
    private JSONObject json(String k1,String v1,String k2,String v2) { JSONObject o=new JSONObject(); try { o.put(k1,v1); o.put(k2,v2); } catch(Exception ignored) {} return o; }
    private static String encode(String value) { try { return URLEncoder.encode(value, "UTF-8"); } catch(Exception e) { return value; } }
    private interface Parser<T> { T parse(String body) throws Exception; }
    private <T> void request(String method, String path, String bearer, JSONObject payload, Parser<T> parser, Callback<T> callback) { new Thread(() -> { HttpURLConnection c=null; try { c=(HttpURLConnection)new URL(baseUrl+path).openConnection(); c.setRequestMethod(method); c.setConnectTimeout(7000); c.setReadTimeout(7000); c.setRequestProperty("Accept", "application/json"); if (bearer != null && !bearer.isEmpty()) c.setRequestProperty("Authorization", "Bearer "+bearer); if (payload != null) { c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json"); try(OutputStream out=c.getOutputStream()) { out.write(payload.toString().getBytes(StandardCharsets.UTF_8)); } } int status=c.getResponseCode(); String body=read(status>=400?c.getErrorStream():c.getInputStream()); if(status<200||status>=300) throw new ApiException(status, body); T value=parser.parse(body); main.post(() -> callback.success(value)); } catch(ApiException e) { main.post(() -> callback.failure(e.status, e.getMessage())); } catch(Exception e) { main.post(() -> callback.failure(-1, e.getMessage()==null?"网络请求失败":e.getMessage())); } finally { if(c!=null)c.disconnect(); } }).start(); }
    private static String read(InputStream in) throws Exception { if(in==null)return ""; StringBuilder s=new StringBuilder(); try(BufferedReader r=new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) { String line; while((line=r.readLine())!=null)s.append(line); } return s.toString(); }
    private static final class ApiException extends Exception { final int status; ApiException(int s,String m){super(m);status=s;} }
}
