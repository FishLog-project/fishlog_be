package com.fishlog.fishlog_be.global.init.dto;

/**
 * 어종 도감 콘텐츠 시드 1건. {@code name}은 {@code fishes.name}(UNIQUE)과 매칭되는 키다.
 *
 * @param name 어종명 — 기존 어종 행을 찾는 기준 키
 * @param habitat 서식지
 * @param description 도감 설명
 */
public record FishContentSeed(String name, String habitat, String description) {}
