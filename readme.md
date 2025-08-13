javac -d out $(find src -name "*.java")
java -cp out game.train.TrainerMain --preset fast