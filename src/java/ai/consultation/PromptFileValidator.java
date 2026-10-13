package ai.consultation;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Build-time validation for the repository-managed System Prompt. */
public final class PromptFileValidator {
    private PromptFileValidator() {}
    public static void main(String[] args) {
        try {
            byte[] bytes=Files.readAllBytes(Paths.get(args[0]));
            String prompt=new String(bytes,StandardCharsets.UTF_8);
            if(!java.util.Arrays.equals(bytes,prompt.getBytes(StandardCharsets.UTF_8)))throw new IllegalArgumentException();
            ConsultationPrompt.validateRepositoryPrompt(prompt);
            System.out.println("Validated repository System Prompt: characters="+prompt.length()+"; API calls=0");
        } catch(Exception e) {
            System.err.println("Invalid repository System Prompt: check UTF-8, size and secret policy. No content logged.");
            System.exit(1);
        }
    }
}
