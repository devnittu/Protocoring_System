package com.examportal.util;

import com.examportal.model.Attempt;
import com.examportal.model.Question;
import com.examportal.model.Response;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Exports exam result data to a CSV file. */
public final class CsvExporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CsvExporter() {}

    /**
     * Exports attempt results including per-question breakdown.
     *
     * @param attempt   the finalised attempt
     * @param questions all questions in the exam
     * @param responses the student's responses
     * @param destFile  output file
     */
    public static void export(Attempt attempt, List<Question> questions,
                              List<Response> responses, File destFile) throws IOException {
        Map<Long, Response> responseMap = responses.stream()
                .collect(Collectors.toMap(Response::questionId, Function.identity()));

        try (CSVWriter writer = new CSVWriter(new FileWriter(destFile))) {

            // Header block
            writer.writeNext(new String[]{"Exam Result Report"});
            writer.writeNext(new String[]{"Attempt ID", String.valueOf(attempt.id())});
            writer.writeNext(new String[]{"Student ID", String.valueOf(attempt.studentId())});
            writer.writeNext(new String[]{"Started At", attempt.startedAt() != null ? attempt.startedAt().format(FMT) : ""});
            writer.writeNext(new String[]{"Submitted At", attempt.submittedAt() != null ? attempt.submittedAt().format(FMT) : ""});
            writer.writeNext(new String[]{"Score", String.valueOf(attempt.score() != null ? attempt.score() : 0)});
            writer.writeNext(new String[]{"Percentage", String.format("%.2f%%", attempt.percentage() != null ? attempt.percentage() : 0)});
            writer.writeNext(new String[]{"Result", Boolean.TRUE.equals(attempt.passed()) ? "PASS" : "FAIL"});
            writer.writeNext(new String[]{"Status", attempt.status().name()});
            writer.writeNext(new String[]{});

            // Column headers
            writer.writeNext(new String[]{"#", "Topic", "Question", "Your Answer", "Correct Answer", "Marks", "Earned", "Result"});

            // Per-question rows
            int idx = 1;
            for (Question q : questions) {
                Response r = responseMap.get(q.id());
                String chosen  = r != null && r.chosenOption() != null ? String.valueOf(r.chosenOption()) : "-";
                boolean correct = r != null && Boolean.TRUE.equals(r.isCorrect());
                writer.writeNext(new String[]{
                    String.valueOf(idx++),
                    q.topic() != null ? q.topic() : "",
                    q.body().replace("\n", " "),
                    chosen,
                    String.valueOf(q.correctOption()),
                    String.valueOf(q.marks()),
                    correct ? String.valueOf(q.marks()) : "0",
                    correct ? "Correct" : "Wrong"
                });
            }
        }
    }
}
