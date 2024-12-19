# Dita velocity language properties

# Making changes

When making changes to any of the language.properties files, make sure that all other properties files are in sync.
This means that all properties files should always contain the exact same keys.

When extending the underlying dita templates or creating new content, it is recommended to work only with the dita/velocity
template itself and fully testing it before copying all contained text strings to the language.properties files.
