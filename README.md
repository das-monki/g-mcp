# G-MCP: Google Workspace MCP Server

An MCP (Model Context Protocol) server for interacting with Google Workspace, specifically Gmail functionality.

## Features

- **Multi-workspace support**: Works with multiple Google Workspace accounts
- **Domain selection**: Supports workspaces with primary and secondary domains
- **Email reading**: Read emails from specified domains with query support
- **Draft creation**: Create email drafts with CC/BCC support

## Setup

### Prerequisites

- Nix with flakes enabled
- Google Cloud Project with Gmail API enabled
- Service account credentials with domain-wide delegation

### Installation

1. Clone the repository:
```bash
git clone git@monki:das-monki/g-mcp.git
cd g-mcp
```

2. Enter the development environment:
```bash
nix develop
```

3. Set up Google credentials:
   - Create a service account in Google Cloud Console
   - Enable Gmail API and Admin SDK API
   - Download the service account key as `credentials.json`
   - Set up domain-wide delegation for the service account
   - Set environment variable: `export GOOGLE_APPLICATION_CREDENTIALS=credentials.json`

### Building

Build the server:
```bash
clojure -T:build uber
```

Or use Nix:
```bash
nix build
```

### Running

Start the MCP server:
```bash
clojure -M:run
```

Or run the built binary:
```bash
./result/bin/g-mcp
```

## MCP Tools

### `list-domains`
List all available domains in the authenticated Google Workspace.

**Parameters:** None

**Returns:** List of domains with their primary status and verification state.

### `read-emails`
Read emails from a specified domain.

**Parameters:**
- `domain` (string): Domain to read emails from
- `max-results` (number, optional): Maximum number of emails to return (default: 10)
- `query` (string, optional): Gmail search query to filter emails

**Returns:** List of emails with metadata and content.

### `create-draft`
Create an email draft in Gmail.

**Parameters:**
- `domain` (string): Domain to create the draft in
- `to` (string): Recipient email address
- `subject` (string): Email subject
- `body` (string): Email body content
- `cc` (string, optional): CC recipients
- `bcc` (string, optional): BCC recipients

**Returns:** Draft ID and confirmation message.

## Configuration

The server requires Google service account credentials with the following scopes:
- `https://www.googleapis.com/auth/gmail.readonly`
- `https://www.googleapis.com/auth/gmail.compose`
- `https://www.googleapis.com/auth/gmail.modify`
- `https://www.googleapis.com/auth/admin.directory.domain.readonly`

## Development

Run tests:
```bash
clojure -M:test
```

Start a REPL:
```bash
clojure -M:repl
```