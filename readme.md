javac -d out $(find src -name "*.java")
java -cp out game.train.TrainerMain --preset fast


java -cp out game.train.TrainerMain --preset fast --gens 15 --episodeMs 8000