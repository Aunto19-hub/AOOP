package com.seamline.security;

/** The email and display name Google vouched for in a verified ID token. */
public record GoogleIdentity(String email, String name) {
}
