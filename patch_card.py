import re
with open('app/src/main/java/com/example/ShoonyaDebugCard.kt', 'r') as f:
    content = f.read()
content = content.replace('DhanWebSocketDebugCard', 'ShoonyaWebSocketDebugCard')
content = content.replace('Dhan WebSocket Debugger', 'Shoonya WebSocket Debugger')
with open('app/src/main/java/com/example/ShoonyaDebugCard.kt', 'w') as f:
    f.write(content)
