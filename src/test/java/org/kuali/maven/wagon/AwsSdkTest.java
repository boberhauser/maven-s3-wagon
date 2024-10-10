package org.kuali.maven.wagon;

import org.junit.Test;
import org.kuali.maven.wagon.auth.AssumeRoleProfileCredentialsProvider;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AwsSdkTest {

    @Test
    public void testXml() {
        // TODO create proper test for settings.xml parsing.
        final String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<configuration>\n" +
                "  <bucket.name>public-bucket-name</bucket.name>\n" +
                "  <aws.profile>aws-profile-name</aws.profile>\n" +
                "</configuration>";

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(input)));

            XPath xPath = XPathFactory.newInstance().newXPath();

            NodeList nodeList = (NodeList) xPath.evaluate("//bucket.name", doc, XPathConstants.NODESET);
            if (nodeList != null && nodeList.getLength() > 0) {
                String bucketName = nodeList.item(0).getTextContent();
                System.out.println(bucketName);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
