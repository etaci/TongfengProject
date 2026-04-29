package com.tongfeng.backend.app;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

	public String next(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().replace("-", "");
	}
}
