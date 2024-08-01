package com.tujuhsembilan.example.configuration.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Validated
@Data
@Component
@ConfigurationProperties(prefix = "application.security")
public class AuthProp {

  @NotBlank
  private String uuid = "b592691e-df3f-4049-86d5-50bdce269354";

  @Min(8)
  private Integer strength = 8;

  private String systemUsername = "SYSTEM";
  private String systemPassword = "$2a$16$Pjg5ZRu.I2TsN5W38PMyQuYBorSrwmRKa/4fc01nEZl0FSOQeou3C";
}
