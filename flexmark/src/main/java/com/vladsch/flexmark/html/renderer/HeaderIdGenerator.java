package com.vladsch.flexmark.html.renderer;

import com.vladsch.flexmark.ast.AnchorRefTarget;
import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.util.AnchorRefTargetBlockVisitor;
import com.vladsch.flexmark.html.HtmlRenderer;

import java.util.HashMap;

public class HeaderIdGenerator implements HtmlIdGenerator {
    HashMap<String, Integer> headerBaseIds = new HashMap<String, Integer>();
    boolean resolveDupes;
    String toDashChars;
    String nonDashChars;
    boolean noDupedDashes;

    @Override
    public void generateIds(Document document) {
        resolveDupes = HtmlRenderer.HEADER_ID_GENERATOR_RESOLVE_DUPES.getFrom(document);
        toDashChars = HtmlRenderer.HEADER_ID_GENERATOR_TO_DASH_CHARS.getFrom(document);
        nonDashChars = HtmlRenderer.HEADER_ID_GENERATOR_NON_DASH_CHARS.getFrom(document);
        noDupedDashes = HtmlRenderer.HEADER_ID_GENERATOR_NO_DUPED_DASHES.getFrom(document);

        new AnchorRefTargetBlockVisitor() {
            @Override
            protected void visit(AnchorRefTarget node) {
                if (node.getAnchorRefId().isEmpty()) {
                    String text = node.getAnchorRefText();
                    String refId = null;

                    refId = generateId(text);

                    if (refId != null) {
                        node.setAnchorRefId(refId);
                    }
                }
            }
        }.visit(document);
    }

    String generateId(final String text) {
        if (!text.isEmpty()) {
            String baseRefId = generateId(text, toDashChars, nonDashChars, noDupedDashes);

            if (resolveDupes) {
                if (headerBaseIds.containsKey(baseRefId)) {
                    int index = headerBaseIds.get(baseRefId);

                    index++;
                    headerBaseIds.put(baseRefId, index);
                    baseRefId += "-" + index;
                } else {
                    headerBaseIds.put(baseRefId, 0);
                }
            }

            return baseRefId;
        }
        return null;
    }

    @Override
    public String getId(Node node) {
        return node instanceof AnchorRefTarget ? ((AnchorRefTarget) node).getAnchorRefId() : null;
    }

    @Override
    public String getId(final CharSequence text) {
        return generateId(text.toString());
    }

    @SuppressWarnings("WeakerAccess")
    public static String generateId(CharSequence headerText, String toDashChars, boolean noDupedDashes) {
        return generateId(headerText, toDashChars, null, noDupedDashes);
    }

    @SuppressWarnings("WeakerAccess")
    public static String generateId(CharSequence headerText, String toDashChars, String nonDashChars, boolean noDupedDashes) {
        int iMax = headerText.length();
        StringBuilder baseRefId = new StringBuilder(iMax);
        if (toDashChars == null) toDashChars = HtmlRenderer.HEADER_ID_GENERATOR_TO_DASH_CHARS.getFrom(null);
        if (nonDashChars == null) nonDashChars = HtmlRenderer.HEADER_ID_GENERATOR_NON_DASH_CHARS.getFrom(null);

        for (int i = 0; i < iMax; i++) {
            char c = headerText.charAt(i);
            if (isAlphabetic(c)) baseRefId.append(Character.toLowerCase(c));
            else if (Character.isDigit(c)) baseRefId.append(c);
            else if (nonDashChars.indexOf(c) != -1) baseRefId.append(c);
            else if (toDashChars.indexOf(c) != -1 && (!noDupedDashes
                    || ((c == '-' && baseRefId.length() == 0)
                    || baseRefId.length() != 0 && baseRefId.charAt(baseRefId.length() - 1) != '-'))
                    ) baseRefId.append('-');
        }
        return baseRefId.toString();
    }

    public static boolean isAlphabetic(final char c) {
        return (((((1 << Character.UPPERCASE_LETTER) |
                (1 << Character.LOWERCASE_LETTER) |
                (1 << Character.TITLECASE_LETTER) |
                (1 << Character.MODIFIER_LETTER) |
                (1 << Character.OTHER_LETTER) |
                (1 << Character.LETTER_NUMBER)) >> Character.getType((int) c)) & 1) != 0);
    }

    public static class Factory implements HeaderIdGeneratorFactory, HtmlIdGeneratorFactory {
        @Override
        public HtmlIdGenerator create(LinkResolverContext context) {
            return new HeaderIdGenerator();
        }

        @Override
        public HtmlIdGenerator create() {
            return new HeaderIdGenerator();
        }
    }
}
