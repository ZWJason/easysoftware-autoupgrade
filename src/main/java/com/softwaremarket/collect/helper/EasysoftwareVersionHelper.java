package com.softwaremarket.collect.helper;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.softwaremarket.collect.config.CollectConfig;
import com.softwaremarket.collect.util.HttpRequestUtil;
import com.softwaremarket.collect.util.JacksonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EasysoftwareVersionHelper {
    private final CollectConfig collectConfig;

    public void getEasysoftVersion(String appName, JSONObject upObj, JSONObject openeulerObj) {
        String projectsInfoUrl = collectConfig.getProjectsInfoUrl();
        String result = HttpRequestUtil.sendGet(projectsInfoUrl + appName, new HashMap<>());
        if (result != null) {
            JSONObject resultObj = JacksonUtils.toObject(JSONObject.class, result);
            if (CollectionUtils.isEmpty(resultObj))
                return;

            JSONArray items = resultObj.getJSONArray("items");
            if (CollectionUtils.isEmpty(items))
                return;

            for (Object item : items) {
                JSONObject o = new JSONObject((Map) item);
                if ("app_up".equals(o.getString("tag"))) {
                    upObj.put("latest_version", o.getString("version"));
                }
                if ("app_openeuler".equals(o.getString("tag"))) {
                    String version = o.getString("version");
                    openeulerObj.put("latest_version", version);
                    openeulerObj.put("name", appName);
                    JSONArray rawVersions = o.getJSONArray("raw_versions");
                    List collect = (List) rawVersions.stream().filter(v -> String.valueOf(v).contains(version)).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(collect))
                        openeulerObj.put("os_version", String.valueOf(collect.get(0)).split(version + "-")[1]);
                }
            }
        }
    }


    public Set<String> getEasysoftApppkgSet() {
        String apppkgInfoUrl = collectConfig.getApppkgInfoUrl();
        HashSet<String> appNameSet = new HashSet<>();
        int currentPage = 1;
        JSONArray list = new JSONArray();
        do {
            HashMap<String, Object> paramMap = new HashMap<>();
            paramMap.put("timeOrder", "asc");
            paramMap.put("name", "apppkg");
            paramMap.put("pageSize", "50");
            paramMap.put("pageNum", currentPage);
            String result = HttpRequestUtil.sendGet(apppkgInfoUrl, paramMap);
            if (result != null) {
                JSONObject resultObj = JacksonUtils.toObject(JSONObject.class, result);
                System.out.println(resultObj);
                JSONObject data = resultObj.getJSONObject("data");
                list = data.getJSONArray("list");
                if (!CollectionUtils.isEmpty(list)) {
                    list.stream().forEach(a -> {
                        JSONObject app = new JSONObject((Map) a);
                        JSONArray children = app.getJSONArray("children");
                        if (!CollectionUtils.isEmpty(children)) {
                            children.stream().forEach(c -> {
                                JSONObject child = new JSONObject((Map) c);
                                if ("loki".equals(String.valueOf(child.get("name")).toLowerCase(Locale.ROOT)))
                                    appNameSet.add(String.valueOf(child.get("name")).toLowerCase(Locale.ROOT));
                            });
                        }
                    });
                    if (list.size() == 50)
                        currentPage++;
                }
            }
        } while (list != null && list.size() == 50);
        return appNameSet;
    }


    public String getOpeneulerLatestOsVersion() {
        String openEulerOsVersionInfoUrl = collectConfig.getOpenEulerOsVersionInfoUrl();
        String result = HttpRequestUtil.sendGet(openEulerOsVersionInfoUrl, new HashMap<>());
        if (result != null) {
            JSONObject resultObj = JacksonUtils.toObject(JSONObject.class, result);
            JSONArray data = resultObj.getJSONArray("data");
            return data.get(0) + "";
        }
        return null;
    }
}