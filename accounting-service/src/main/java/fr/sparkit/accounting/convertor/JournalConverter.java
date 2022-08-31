package fr.sparkit.accounting.convertor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import fr.sparkit.accounting.dto.JournalDto;
import fr.sparkit.accounting.entities.Journal;

public final class JournalConverter {
    private JournalConverter() {
        super();
    }

    public static Journal dtoToModel(JournalDto journalDto) {
        return new Journal(journalDto.getId(), journalDto.getCode(), journalDto.getLabel(), journalDto.getCreatedDate(),
                journalDto.isReconcilable(), journalDto.isCashFlow());
    }

    public static JournalDto modelToDto(Journal journal) {
        return new JournalDto(journal.getId(), journal.getCode(), journal.getLabel(), journal.getCreatedDate(),
                journal.isReconcilable(), journal.isCashFlow());
    }

    public static List<JournalDto> modelsToDtos(Collection<Journal> journals) {
        return journals.stream().filter(Objects::nonNull).map(JournalConverter::modelToDto)
                .collect(Collectors.toList());
    }

}
