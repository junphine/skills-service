/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import skills.example.utils.SecretsUtil;

import javax.net.ssl.HttpsURLConnection;

@SpringBootApplication
public class SkillsExampleApplication implements WebMvcConfigurer {

	static final String DISABLE_HOSTNAME_VERIFIER_PROP = "skills.disableHostnameVerifier";

	public static void main(String[] args) {
		boolean disableHostnameVerifier = Boolean.parseBoolean(System.getProperty(DISABLE_HOSTNAME_VERIFIER_PROP));
		if (disableHostnameVerifier) {
			HttpsURLConnection.setDefaultHostnameVerifier((s, sslSession) -> true);
		}

		// allows the secrets to be passed in via file instead of using command line or env variables
		new SecretsUtil().updateSecrets();

		SpringApplication.run(SkillsExampleApplication.class, args);
	}

}
