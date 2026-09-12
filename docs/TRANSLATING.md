# Translating Printer

For 0.1.0, English is bundled and this contribution workflow is the translation deliverable. Additional languages ship after review by a fluent speaker; placeholder machine translations are not release assets.

1. Copy `src/main/resources/assets/printer/lang/en_us.json` to a file alongside it named for the Minecraft locale, for example `de_de.json`.
2. Translate values only. Keep keys, UTF-8 encoding, valid JSON, and the number/order of `%s` placeholders. A placeholder may contain a translated component such as a print mode or Shift key. Avoid concatenating fragments that impose English grammar. Each item's `tooltip.summary` is always visible; numbered `tooltip.conditionN` headings and `tooltip.behaviourN` detail lines are Shift-only. Conditions use gold emphasis and behaviours use gray consistently in every language; do not insert formatting codes into translations.
3. Keep tooltips short enough for the inventory. Translate both the summary and expanded details, empty states, background-color/mode names, statuses, and actionable failure messages. Player-entered titles remain player text.
4. Run `./gradlew.bat test`. Compare your key set with English and report any missing source strings. English may gain keys while 0.1.0 is in development; refresh the translation before review.
5. Switch to the target language in Minecraft. Check the creative tab, all items with and without Shift, printer GUI at multiple GUI scales, color and monochrome prints, background-color choices, error messages, and advancements. Check that `%s` placeholders render values and that text does not overlap or clip.
6. Submit the language file with the locale, fluent-reviewer credit (with their consent), tested mod version, and notes about terminology/layout. Contributions can be supplied as a repository pull request or a patch for maintainer review. Translations follow the repository's MIT license.

Do not put image URLs, personal information, or test credentials in translations or review screenshots. If a translated value needs a different placeholder order, coordinate a source-string change rather than silently dropping a value.
