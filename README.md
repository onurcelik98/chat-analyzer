# chat-analyzer
A very simple text analysis implementation that is written for very special purposes.

# About this project

This project is a brief code snippet consisting of a single Java class that;

- reads a textual input file,
- applies some preprocessing steps to its contents,
- does token frequency analysis to it
- writes the results to output files.

It uses the [zemberek-nlp](https://github.com/ahmetaa/zemberek-nlp) library developed for natural language tasks in Turkish, benefiting mainly from its text normalization features.

The focus of the project is analyzing TXT-exported WhatsApp chats, so the preprocessing steps are shaped around this. Although it can theoretically work with any textual input, it is essentially a single-purpose implementation that is not meant to be used for more general purposes.

The input file format, as already mentioned above, is a .txt file (more specifically an exported WhatsApp chat history). The initial output format is the tokens in the normalized text and their number of occurrences, sorted by frequency. This will look like the following:

```
lorem=92
amet=87
elit=55
ipsum=55
asipiscing=46
sit=43
dolor=38
consectetur=24
...
```

One can also generate a second output, where instead of the number of occurrences of each token being denoted, the token itself is repeated that many times consecutively in its corresponding line. This is to provide a representation of the same data that can be used with services like [wordclouds.com](https://www.wordclouds.com/) more conveniently.
