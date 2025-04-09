package com.example.capstone02.oauth;

import com.example.capstone02.model.User;

import java.util.Map;

public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String picture;
    private String socialId;
    private String provider;

    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name,
                           String email, String picture, String socialId, String provider) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.socialId = socialId;
        this.provider = provider;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        return ofKakao(userNameAttributeName, attributes);
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        return new OAuthAttributes(
                attributes,
                userNameAttributeName,
                (String) properties.get("nickname"),
                kakaoAccount != null && (Boolean) kakaoAccount.get("has_email") ? (String) kakaoAccount.get("email") : null,
                (String) properties.get("profile_image"),
                attributes.get(userNameAttributeName).toString(),
                "kakao"
        );
    }

    public User toEntity() {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setProfileImage(picture);
        user.setSocialId(socialId);
        user.setProvider(provider);
        return user;
    }

    // Getters
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public String getNameAttributeKey() {
        return nameAttributeKey;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPicture() {
        return picture;
    }

    public String getSocialId() {
        return socialId;
    }

    public String getProvider() {
        return provider;
    }
}