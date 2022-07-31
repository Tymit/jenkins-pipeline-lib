def call(String level, String message) {
    String GREEN = "\033[32m"
    String BLUE = "\033[34m"
    String RED = "\033[31m"
    String YELLOW = "\033[33m"

    String INFO = "INFO"
    String WARNING = "WARNING"
    String ERROR = "ERROR"
    String TRACE = "TRACE"

    String color = ""

    switch(level) {
        case INFO:
            echo "${GREEN}[${level}]\033[0m ${message}"
            break;
        case WARNING:
            echo "${YELLOW}[${level}]\033[0m ${message}"
            break;
        case ERROR:
            echo "${RED}[${level}]\033[0m ${message}"
            break;
        case TRACE:
            echo "${BLUE}[${level}]\033[0m ${message}"
            break;
        default:
            echo "${BLUE}[INFO]\033[0m ${message}"
            break;
    }
}
