package fr.sparkit.accounting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ReportTemplateDefaultParameters {
    private String companyName;
    private String logoDataBase64;
    private String companyAdressInfo;
    private boolean provisionalEdition;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime generationDate;
    private String commercialRegister;
    private String matriculeFisc;
    private String mail;
    private String webSite;
    private String tel;

}
