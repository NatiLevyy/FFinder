const Anthropic = require('@anthropic-ai/sdk');

const anthropic = new Anthropic({
  apiKey: process.env.ANTHROPIC_API_KEY,
});

async function askClaude(prompt) {
  try {
    const message = await anthropic.messages.create({
      model: 'claude-3-opus-20240229', // או 'claude-3-sonnet-20240229'
      max_tokens: 1024,
      messages: [{ role: 'user', content: prompt }],
    });
    
    console.log('\n🤖 Claude Response:\n');
    console.log(message.content[0].text);
  } catch (error) {
    console.error('Error:', error);
  }
}

// קבל input מה-command line
const args = process.argv.slice(2);
if (args.length > 0) {
  const question = args.join(' ');
  askClaude(question);
} else {
  console.log('Usage: node claude-helper.js "your question here"');
}