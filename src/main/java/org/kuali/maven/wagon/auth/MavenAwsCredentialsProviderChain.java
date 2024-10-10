/**
 * Copyright 2010-2015 The Kuali Foundation
 * Copyright 2018 Sean Hennessey
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl2.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.maven.wagon.auth;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import software.amazon.awssdk.auth.credentials.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * This chain searches for AWS credentials in system properties -&gt; environment variables -&gt; ~/.m2/settings.xml
 * -&gt; AWS Configuration Profile -&gt; Amazon's EC2 Container Service/EC2 Instance Metadata Service
 */
public interface MavenAwsCredentialsProviderChain {

	static AwsCredentialsProviderChain getProviderChain(MavenSession session, String bucketName) {
		Logger log = LoggerFactory.getLogger(MavenAwsCredentialsProviderChain.class);

		Optional<String> profileName = getAwsProfileForBucket(session, bucketName);
		if (profileName.isPresent()) {
			log.info("Using AWS profile: " + profileName);
		}

		AwsCredentialsProvider profileCredentialsProvider = profileName.isPresent()
				? new AssumeRoleProfileCredentialsProvider(profileName.get())
				: ProfileCredentialsProvider.create(); // default profile

		return AwsCredentialsProviderChain.builder()
				// System properties always win
				.addCredentialsProvider(SystemPropertyCredentialsProvider.create())

				// Then fall through to environment variables
				.addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())

				// Then fall through to IAM roles for service accounts (IRSA)
				.addCredentialsProvider(WebIdentityTokenFileCredentialsProvider.create())

				// TODO .addCredentialsProvider(AuthenticationInfoCredentialsProvider)

				// Then fall through to reading the ~/.aws/credentials and ~/.aws/config files many people use.
				.addCredentialsProvider(profileCredentialsProvider)

				// TODO .addCredentialsProvider(EC2ContainerCredentialsProviderWrapper)
				.build();
	}

	static Optional<String> getAwsProfileForBucket(MavenSession session, String bucketName) {
		final Logger log = LoggerFactory.getLogger(MavenAwsCredentialsProviderChain.class);
		XPath xPath = XPathFactory.newInstance().newXPath();

		log.info("Looking for server configuration of bucket: " + bucketName);

		for (Server server : session.getSettings().getServers()) {
			if (server.getConfiguration() != null) {
				try {
					DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					Document doc = builder.parse(
							new ByteArrayInputStream(server.getConfiguration().toString().getBytes(StandardCharsets.UTF_8)));

					NodeList nodeList = (NodeList) xPath.evaluate("//bucket.name", doc, XPathConstants.NODESET);
					if (nodeList != null && nodeList.getLength() > 0) {
						if (bucketName.equalsIgnoreCase(nodeList.item(0).getTextContent())) {
							NodeList awsProfiles = (NodeList) xPath.evaluate("//aws.profile", doc, XPathConstants.NODESET);
							if (awsProfiles != null && awsProfiles.getLength() > 0) {
								return Optional.of(awsProfiles.item(0).getTextContent());
							}
						}
					}
				}
				catch (Exception e) {
					log.error("Unable to parse XML: " + e.getMessage());
				}
			}
		}

		return Optional.empty();
	}
}
