package com.ballastlane.pokedex.adapter.in.web.error;

/** One entry of a problem's {@code errors[]}: which input field is wrong and why. */
public record ProblemFieldError(String field, String message) {}
