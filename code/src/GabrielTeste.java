import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

//! ESSA CLASSE É APENAS BACKUP DA FUNC 6 FUNCIONAL, JA MUDEI MUITO O CAMINHO NA CLASSE GabrielTesteFunc2
public class GabrielTeste {
    public static void main(String[] args) {
        String csvPath = "Input/TesteFuncao2-3/csv/image_001TESTE.csv";
        double[][] originalMatrix = readCSVToArray(csvPath);

        // Dimensão e número de eigenfaces
        int numOfColumns = originalMatrix[0].length; // Número de colunas (64)
        int eigenFaces = numOfColumns; // ajuste conforme necessário, mas se tiver colunas = eigenfaces imagem fica melhor

        print_Matrix(originalMatrix, "originalMatrix");
        System.out.println("Número de colunas: " + numOfColumns);
        System.out.println("Número de eigenfaces: " + eigenFaces);

        //* 1. Cálculo da média - revisto
        double[][] averageColumn = averageColumn(originalMatrix);
        adjustPrecision(averageColumn);
        print_Matrix(averageColumn, "averageColumn");

        //* 2. Centralização (Matriz de desvios) - revisto
        double[][] deviantMatrix = deviantMatrix(originalMatrix, averageColumn);
        adjustPrecision(deviantMatrix);
        print_Matrix(deviantMatrix, "deviantMatrix");

        double[][] deviantMatrixTranspose = transposeMatrix(deviantMatrix);
        adjustPrecision(deviantMatrixTranspose);
        print_Matrix(deviantMatrixTranspose, "deviantMatrixTranspose");

        //* 3. Matriz de Covariância - revisto
        double[][] covariance = covariances(deviantMatrix, numOfColumns);
        adjustPrecision(covariance);
        print_Matrix(covariance, "covariance");

        //* 4. Valores e vetores próprios - revisto
        double[][] eigenValuesATxA = eigenValues(deviantMatrixTranspose, deviantMatrix);
        adjustPrecision(eigenValuesATxA);
        print_Matrix(eigenValuesATxA, "eigenValuesATxA");

        double[][] eigenVectorsATxA = eigenVectors(deviantMatrixTranspose, deviantMatrix);
        adjustPrecision(eigenVectorsATxA);
        print_Matrix(eigenVectorsATxA, "eigenVectorsATxA");

        double[][] eigenVectorsAxAT = eigenVectors(deviantMatrix, deviantMatrixTranspose);
        adjustPrecision(eigenVectorsAxAT);
        print_Matrix(eigenVectorsAxAT, "eigenVectorsAxAT");

        double[][] eigenValuesAxAT = eigenValues(deviantMatrix, deviantMatrixTranspose);
        adjustPrecision(eigenValuesAxAT);
        print_Matrix(eigenValuesAxAT, "eigenValuesAxAT");

        double[][] normalizedVectorsAxAT = normalizeVectors(eigenVectorsAxAT);
        adjustPrecision(normalizedVectorsAxAT);
        print_Matrix(normalizedVectorsAxAT, "normalizedVectorsAxAT");

        //* 5. Centralização da imagem para reconstrução - ATENÇÃO!
        //! revisar aqui pois preciso confirmar teoria para mais imagens, para uma funciona
        double[] linearizedOriginal = matrixToArray1D(originalMatrix);
        System.out.println("linearizedOriginal lenght: " + linearizedOriginal.length);
        System.out.println("linearizedOriginal: " + Arrays.toString(linearizedOriginal));

        //* Calcula o vetor médio para o vetor linearizado - ATENÇÃO!
        //! revisar aqui pois o metodo meanVector recebe averageColumn mas averageColumn é a media de cada linha
        //! e não de cada coluna
        double[] meanVector = matrixToArray1D(originalMatrix);
        System.out.println("meanVector lenght: " + meanVector.length);
        System.out.println("meanVector: " + Arrays.toString(meanVector));

        //* Centraliza a imagem original linearizada - ATENÇÃO!
        //! pois depende de meanVector que precisa ser revisto
        double[] phi = centralizeImage(linearizedOriginal, meanVector);
        System.out.println("phi lenght: " + phi.length);
        System.out.println("phi: " + Arrays.toString(phi));

        //* 6. Cálculo dos pesos (projeção nos eigenfaces) - ATENÇÃO!
        double[] weights = calculateWeights(phi, normalizedVectorsAxAT);
        System.out.println("weights lenght: " + weights.length);
        System.out.println("weights: " + Arrays.toString(weights));

        //* 7. Reconstrução da imagem com eigenFaces eigenfaces - ATENÇÃO!
        double[] reconstructedImage = reconstructImage(meanVector, normalizedVectorsAxAT, weights, eigenFaces);
        System.out.println("reconstructedImage lenght: " + reconstructedImage.length);
        System.out.println("reconstructedImage: " + Arrays.toString(reconstructedImage));

        //* 8. Conversão para matriz 2D
        double[][] reconstructedImageMatrix = array1DToMatrix(reconstructedImage, originalMatrix);
        adjustPrecision(reconstructedImageMatrix);
        print_Matrix(reconstructedImageMatrix, "reconstructedImageMatrix");

        //* 9. Salvar a imagem reconstruída
        saveImage(reconstructedImageMatrix, csvPath, "Input/TesteFuncao2-3/OutputImagesFunc2-3");

        //! Print para verificar cada etapa


    }

    public static double[] matrixToArray1D(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[] array = new double[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                array[i * cols + j] = matrix[i][j];
            }
        }
        return array;
    }

    public static double[] extractMeanVector(double[][] averageColumn) {
        double[] meanVector = new double[averageColumn[0].length];
        for (int i = 0; i < averageColumn[0].length; i++) {
            meanVector[i] = averageColumn[0][i]; //! averageColumn revisar
        }
        return meanVector;
    }

    public static void saveImage(double[][] imageArray, String inputCsvPath, String outputFolderPath) {
        int height = imageArray.length;
        int width = imageArray[0].length;

        // Normalize the image to the range 0-255
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        // Find min and max values
        for (double[] row : imageArray) {
            for (double val : row) {
                if (val < min) min = val;
                if (val > max) max = val;
            }
        }

        int[][] normalizedImage = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                normalizedImage[y][x] = (int) ((imageArray[y][x] - min) / (max - min) * 255);
            }
        }

        // Create output file name
        String csvFileName = new File(inputCsvPath).getName();
        String pngFileName = csvFileName.replace(".csv", ".png");
        String outputPath = outputFolderPath + "/" + pngFileName;

        // Write the image
        try {
            writeArrayAsImage(normalizedImage, outputPath);
            System.out.println("Image saved successfully: " + outputPath);
        } catch (IOException e) {
            System.err.println("Failed to save the image: " + e.getMessage());
        }
    }

    public static double[] centralizeImage(double[] image, double[] meanVector) {
        if (image.length != meanVector.length) {
            throw new IllegalArgumentException("Os tamanhos dos arrays 'image' e 'meanVector' não correspondem.");
        }
        double[] phi = new double[image.length];
        for (int i = 0; i < image.length; i++) {
            phi[i] = image[i] - meanVector[i];
        }
        return phi;
    }

    //! se para calcular os pesos pega phi e eigenfaces, revisar pois esta recebendo uma matriz
    //! de vetores normalizados e não a quantidade de eigenfaces (que nesse caso é a mesma qtd de colunas)
    public static double[] calculateWeights(double[] phi, double[][] eigenfaces) {
        if (Math.sqrt(phi.length) != eigenfaces.length) {
            throw new IllegalArgumentException("O comprimento de 'phi' deve ser igual ao número de linhas em 'eigenfaces'.");
        }
        double[] weights = new double[eigenfaces[0].length];
        for (int j = 0; j < eigenfaces[0].length; j++) {
            for (int i = 0; i < eigenfaces[0].length; i++) {
                weights[j] += phi[i] * eigenfaces[i][j];
            }
        }
        return weights;
    }

    //! revisar essa matriz que recebe chamada eigenFaces
    public static double[] reconstructImage(double[] meanVector, double[][] eigenfaces, double[] weights, int quantityEigenfaces) {
        quantityEigenfaces = Math.min(quantityEigenfaces, eigenfaces[0].length);

        double[] reconstructed = new double[meanVector.length];
        for (int i = 0; i < meanVector.length; i++) {
            reconstructed[i] = meanVector[i];
        }

        for (int j = 0; j < quantityEigenfaces; j++) {
            for (int i = 0; i < eigenfaces[0].length; i++) {
                reconstructed[i] += weights[j] * eigenfaces[i][j];
            }
        }

        return reconstructed;
    }

    public static double[][] array1DToMatrix(double[] array, double[][] originalMatrix) {
        int rows = originalMatrix.length;
        int columns = originalMatrix[0].length;
        if (array.length != rows * columns) {
            throw new IllegalArgumentException("O tamanho do vetor não corresponde às dimensões da matriz.");
        }
        double[][] matrix = new double[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrix[i][j] = array[i * columns + j];
            }
        }
        return matrix;
    }

    public static void writeArrayAsImage(int[][] array, String outputFilePath) throws IOException {
        int height = array.length;
        int width = array[0].length;

        // Create a BufferedImage
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // Set the pixel intensities
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int intensity = array[y][x];
                if (intensity < 0 || intensity > 255) {
                    throw new IllegalArgumentException("Pixel intensity must be between 0 and 255.");
                }
                int rgb = (intensity << 16) | (intensity << 8) | intensity; // Set the same value for R, G, B
                image.setRGB(x, y, rgb);
            }
        }

        // Write the image to the file
        File outputFile = new File(outputFilePath);
        ImageIO.write(image, "png", outputFile);
    }

    private static double[][] readCSVToArray(String filePath) {
        ArrayList<double[]> rows = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    String[] values = line.split(",");
                    double[] row = new double[values.length];
                    for (int i = 0; i < values.length; i++) {
                        row[i] = Double.parseDouble(values[i].trim());
                    }
                    rows.add(row);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Erro: Arquivo CSV não encontrado no caminho: " + filePath, e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Erro: Valores inválidos encontrados no arquivo CSV.", e);
        }

        // Converte a lista de linhas em uma matriz
        double[][] matrix = new double[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            matrix[i] = rows.get(i);
        }
        return matrix;
    }

    public static double calculateEAM(double[][] A, double[][] Ak) {
        int M = A.length;
        int N = A[0].length;
        double erroAbsMed = 0;
        // Percorre cada elemento da matriz
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                erroAbsMed += Math.abs(A[i][j] - Ak[i][j]);
            }
        }

        // Calcula o erro médio
        return erroAbsMed / (M * N);
    }

    private static void print_Line(int length, String pattern) {
        for (int i = 0; i < length; i++) {
            System.out.print(pattern);
        }
        System.out.println();
    }

    private static void print_Matrix(double[][] matrixToPrint, String matrixName) {
        System.out.println("Matriz: " + matrixName + " ↓");
        print_Line(matrixToPrint[0].length, "____________");

        for (double[] row : matrixToPrint) {
            System.out.print("|");
            for (int i = 0; i < row.length; i++) {
                System.out.printf("%8.3f\t", row[i]);
                if (i == row.length - 1) {
                    System.out.print("|");
                }
            }
            System.out.println();
        }
        print_Line(matrixToPrint[0].length, "============");
        System.out.println();
    }

    public static boolean testCalculateEAM(double[][] A, double[][] Ak, double expectedEAM) {
        // Chama a função que calcula o EAM
        double calculatedEAM = calculateEAM(A, Ak);

        // Compara o valor calculado com o esperado
        return calculatedEAM == expectedEAM;
    }

    //! esta calculando a media de cada linha, e nao de cada coluna
    public static double[][] averageColumn (double[][] matrix) {
        int rows = matrix.length;
        int columns = matrix[0].length;
        double[][] averageColumn = new double[rows][1];
        for (int i = 0; i < rows; i++) {
            double sum = 0;
            for (int j = 0; j < columns; j++) {
                sum = sum + matrix[i][j];
            }
            averageColumn[i][0] = sum/columns;
        }
        return averageColumn;
    }
    public static boolean testColunaMedia(double[] matrix, double[] expectedColMedia) {
        return Arrays.equals(matrix, expectedColMedia);
    }

    public static double[][] deviantMatrix (double[][] matrix, double[][] averageColumn) {
        int columns = matrix[0].length;
        int rows = matrix.length;
        double[][] deviant = new double[rows][columns];

        for (int j = 0; j < columns; j++) {
            for (int i = 0; i < rows; i++) {
                deviant[i][j] =  deviantColumn(matrix[i][j], averageColumn[i][0]); //! averageColumn[i][0] é a media de cada linha
            }

        }
        return deviant;
    }

    public static double deviantColumn(double matrixValue, double averageColumnValue) {

        double deviant = matrixValue - averageColumnValue; // Calcula o desvio de cada elemento da matriz em relação à média da linha correspondente

        return deviant;
    }

    public static boolean testCalDesvios(double[][] matrix, double[][] expectedDesvios) {
        return Arrays.equals(matrix, expectedDesvios);
    }

    public static double[][] covariances(double[][] A, int N) {
        double[][] AT = transposeMatrix(A);
        double[][] AAT = multiplyMatrix(A,AT);
        return multiplyMatrixByScalar(AAT,1.0/N);
    }

    public static boolean testCovariancia(double[][] C, double[][] expectedC) {
        return C == expectedC;
    }


    public static double[][] eigenVectors (double[][] matrixOne ,double[][] matrixTwo) {
        double[][] matrixThree = multiplyMatrix(matrixOne, matrixTwo);
        EigenDecomposition eigenDecomposition = decomposeMatrix(matrixThree);
        RealMatrix decomposedMatrixThree = eigenDecomposition.getD();
        return decomposedMatrixThree.getData();
    }

    public static double[][] eigenValues (double[][] matrixOne ,double[][] matrixTwo) {
        double[][] matrixThree = multiplyMatrix(matrixOne, matrixTwo);
        EigenDecomposition eigenDecomposition = decomposeMatrix(matrixThree);
        RealMatrix decomposedMatrixThree = eigenDecomposition.getV();
        return decomposedMatrixThree.getData();
    }

    public static double[][] vetoresPropriosATxA(double[][] A,double[][] AT) {
        double[][] ATxA = multiplyMatrix(AT,A);
        EigenDecomposition eigenDecomposition = decomposeMatrix(ATxA);
        RealMatrix V = eigenDecomposition.getV();
        double[][] vi = V.getData();
        return vi;
    }

    public static double[][] vetoresPropriosAxAT(double[][] A,double[][] AT) {
        double[][] AxAT = multiplyMatrix(A,AT);
        EigenDecomposition eigenDecomposition = decomposeMatrix(AxAT);
        RealMatrix V = eigenDecomposition.getV();
        double[][] ui = V.getData();
        return ui;
    }

    public static double[][] valoresPropriosAxAT(double[][] A,double[][] AT) {
        double[][] AxAT = multiplyMatrix(A,AT);
        EigenDecomposition eigenDecomposition = decomposeMatrix(AxAT);
        RealMatrix D = eigenDecomposition.getD();
        double[][] dArray = D.getData();
        adjustPrecision(dArray);
        return dArray;
    }

    //ignora valores negativos muiiiito baixos, para nao obter -0.0
    private static void adjustPrecision(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                if (Math.abs(matrix[i][j]) < 0.001) {
                    matrix[i][j] = 0.0;
                }
            }
        }
    }

    public static double[][] valoresPropriosC(double[][] valoresPropriosAxAT, int N) {
        double[][] lambdai = multiplyMatrixByScalar(valoresPropriosAxAT,1.0/N);
        return lambdai;
    }

    public static double[][] normalizeVectors (double[][] vetoresPropriosC) {
        for (int i = 0; i < vetoresPropriosC[0].length; i++) {
            double[] vetorAtual = new double[vetoresPropriosC.length];

            for (int j = 0; j < vetoresPropriosC.length; j++) {
                vetorAtual[j] = vetoresPropriosC[j][i];
            }

            double norm = calculateNorm(vetorAtual);

            for (int j = 0; j < vetoresPropriosC.length; j++) {
                vetoresPropriosC[j][i] = vetorAtual[j] / norm;
            }

        }
        return vetoresPropriosC;
    }

    private static double calculateNorm(double[] vector) {
        double sum = 0;
        for (int i = 0; i < vector.length; i++) {
            sum = sum + vector[i] * vector[i];
        }
        return Math.sqrt(sum);
    }

    public static EigenDecomposition decomposeMatrix(double[][] matrix) {
        Array2DRowRealMatrix matrixToDecompose = new Array2DRowRealMatrix(matrix);
        return new EigenDecomposition(matrixToDecompose);
    }

    public static double[][] transposeMatrix(double[][] matrix) {
        double[][] transposedMatrix = new double[matrix[0].length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                transposedMatrix[j][i] = matrix[i][j];
            }
        }
        return transposedMatrix;
    }

    public static double[][] multiplyMatrix(double[][] matrixLeft, double[][] matrixRight) {
        double[][] resultMatrix = new double[matrixLeft.length][matrixRight[0].length];
        for (int i = 0; i < matrixLeft.length; i++) {
            for (int j = 0; j < matrixRight[0].length; j++) {
                for (int k = 0; k < matrixRight.length; k++) {
                    resultMatrix[i][j] += matrixLeft[i][k] * matrixRight[k][j];
                }
            }
        }
        return resultMatrix;
    }

    public static double[][] multiplyMatrixByScalar(double[][] matrix, double scalar) {
        double[][] resultMatrix = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                resultMatrix[i][j] = matrix[i][j] * scalar;
            }
        }
        return resultMatrix;
    }
}