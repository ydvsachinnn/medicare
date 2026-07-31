package pep.com.pepclass.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class TextSplitter {

    private static final int MAX_CHUNKS = 500; // Safety cap to prevent runaway allocation

    public List<String> split(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        // Ensure overlap is strictly less than chunkSize to guarantee forward progress
        if (overlap >= chunkSize) {
            overlap = chunkSize / 2;
        }

        int length = text.length();
        int start = 0;

        while (start < length && chunks.size() < MAX_CHUNKS) {
            int end = Math.min(start + chunkSize, length);

            // If we are not at the end, try to find a natural sentence boundary within the overlap zone
            if (end < length) {
                int searchStart = Math.max(start + 1, end - overlap);
                int bestSplitIndex = -1;

                for (int i = end - 1; i >= searchStart; i--) {
                    char c = text.charAt(i);
                    if (c == '.' || c == '!' || c == '?' || c == '\n') {
                        bestSplitIndex = i + 1;
                        break;
                    }
                }

                if (bestSplitIndex > start) {
                    end = bestSplitIndex;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // Calculate next start position with overlap for context continuity
            int nextStart = end - overlap;

            // CRITICAL: Guarantee forward progress — always advance by at least 1 character
            if (nextStart <= start) {
                nextStart = end;
            }

            start = nextStart;
        }

        return chunks;
    }
}
