package org.kuali.maven.wagon.auth;

import org.apache.commons.lang.RandomStringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.util.Optional;

public class AssumeRoleProfileCredentialsProvider implements AwsCredentialsProvider {
    private final String profileName;

    public AssumeRoleProfileCredentialsProvider() {
        this.profileName = null;
    }

    public AssumeRoleProfileCredentialsProvider(String profileName) {
        this.profileName = profileName;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        Optional<Region> region = getRegion(profileName);
        Optional<String> roleArn = getRoleArn(profileName);
        Optional<String> sourceProfile = getSourceProfile(profileName);

        if (!region.isPresent()) {
            throw new IllegalStateException("No region configured for profile " + profileName);
        }

        AwsCredentialsProvider credentialsProvider;
        if (roleArn.isPresent()) {
            AwsCredentialsProvider profileCredentialsProvider = sourceProfile.isPresent()
                    ? ProfileCredentialsProvider.create(sourceProfile.get())
                    : ProfileCredentialsProvider.create(profileName);

            StsClient stsClient = StsClient.builder()
                    .region(region.get())
                    .credentialsProvider(profileCredentialsProvider)
                    .build();

            credentialsProvider = StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(AssumeRoleRequest.builder()
                            .roleArn(roleArn.get())
                            .roleSessionName("maven-resolve-dependencies-session-" + getRandomString(10))
                            .build())
                    .build();
        }
        else {
            credentialsProvider = ProfileCredentialsProvider.create(profileName);
        }

        return credentialsProvider.resolveCredentials();
    }

    public static Optional<String> getRoleArn(String profileName) {
        return getPropertyRecursive(profileName, "role_arn");
    }

    public static Optional<Region> getRegion(String profileName) {
        return getPropertyRecursive(profileName, "region")
                .map(Region::of);
    }

    public static Optional<String> getSourceProfile(String profileName) {
        Optional<Profile> profile = ProfileFile.defaultProfileFile().profile(profileName);
        if (profile.isPresent()) {
            if (profile.get().properties().containsKey("source_profile")) {
                return Optional.of(profile.get().properties().get("source_profile"));
            }
        }
        return Optional.empty();
    }

    private static Optional<String> getPropertyRecursive(String profileName, String propertyName) {
        Optional<Profile> profile = ProfileFile.defaultProfileFile().profile(profileName);
        if (profile.isPresent()) {
            if (profile.get().properties().containsKey(propertyName)) {
                return Optional.of(profile.get().properties().get(propertyName));
            }

            // if the profile is configured to have a source profile, also check that one
            Optional<String> sourceProfile = getSourceProfile(profileName);
            if (sourceProfile.isPresent()) {
                return getPropertyRecursive(sourceProfile.get(), propertyName);
            }
        }

        return Optional.empty();
    }

    private static String getRandomString(int length) {
        boolean useLetters = true;
        boolean useNumbers = false;
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }
}
