const commands = {commands: []}

function getRandomIntPos(max) {
  return Math.floor(Math.random() * max)
}
function getRandomInt(max) {
  return Math.floor(Math.random() * max*2)-max
}

for (let i = 0; i < 300; i++) {
  commands.commands.push(JSON.parse(`{"command":"execute at $PLAYERNAME run summon minecraft:frog ~${getRandomInt(2)} ~20 ~${getRandomInt(2)}","delayTicks":${getRandomIntPos(200)}}`))
}

console.log(JSON.stringify(commands))
