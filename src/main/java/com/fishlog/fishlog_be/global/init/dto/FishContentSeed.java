package com.fishlog.fishlog_be.global.init.dto;

/**
 * 어종 도감 콘텐츠 시드 1건. {@code name}은 {@code fishes.name}(UNIQUE)과 매칭되는 키다.
 *
 * @param name 어종명 — 기존 어종 행을 찾는 기준 키
 * @param habitat 서식지
 * @param description 도감 설명
 * @param rarity 희귀도 문자열(대소문자 무관, 예 {@code "low"}/{@code "usually"}/{@code "high"}). 로더에서 {@link
 *     com.fishlog.fishlog_be.domain.fish.entity.Rarity}로 변환한다. 비었거나 알 수 없으면 null.
 */
public record FishContentSeed(String name, String habitat, String description, String rarity) {}
