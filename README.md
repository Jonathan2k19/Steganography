# Steganography
    ENCRYPTION:
        -general: the goal is to hide a text message inside of an image and replace the 2 least significant bits of the rgb color channels with
                  the message bits to achieve that
        -input: an 24-bit-image (enter path as parameter of prepImage()) and a message (user input)
                    ->can't enter a new line character inside the message due to user input scanner (executes if you press the enter key)
        -output: new image ("SecretImg.png")
    DECRYPTION:
        -general: the goal is to extract the text message back out of the img
        -input: "SecretImg.png"
        -output: message (sometimes the last or last few characters are wrong, haven´t found the fault yet)
