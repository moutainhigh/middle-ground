package com.cloud.log.service;

import java.util.Map;

import com.cloud.model.common.Page;
import com.cloud.model.log.Log;

public interface LogService {

	void save(Log log);

	Page<Log> findLogs(Map<String, Object> params);

}
