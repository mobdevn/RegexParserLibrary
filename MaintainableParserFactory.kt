object SequentialPageTransformationFactory {
    
    fun createTransformation(
        productRegex: String,
        textSeparatorLiteral: String,
        contentType: String?,
        helpText: String?
    ): SequentialPageTransformation {
        val investmentProdLiteral = SequentialPageUtilities.getInputLiteral(productRegex)
        
        return SequentialPageTransformation(
            type = contentType ?: "",
            productLiteral = investmentProdLiteral,
            prodIndex = SequentialPageUtilities.getLiteralIndex(productRegex, investmentProdLiteral),
            transformationIndex = SequentialPageUtilities.getLiteralIndex(productRegex, textSeparatorLiteral),
            outputTransformation = SequentialPageUtilities.getTransformationType(
                contentType = investmentProdLiteral,
                inputTextSeparator = textSeparatorLiteral,
                transformationIndex = SequentialPageUtilities.getLiteralIndex(productRegex, textSeparatorLiteral),
                maxLength = SequentialPageUtilities.getMaxLengthFromRegex(productRegex)
            ),
            transformationLiteral = textSeparatorLiteral,
            maxLength = SequentialPageUtilities.getMaxLengthFromRegex(productRegex)
        )
    }
}
