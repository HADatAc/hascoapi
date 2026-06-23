# Universal VM Deployment Scripts

## Overview

These scripts provide universal VM deployment and cleanup capabilities for **any** ESS, INFRA, or KG component. They automatically detect the component from the directory they're run in and extract all necessary configuration from `infra-config.json`.

## Scripts

### 1. deploy-to-vm-universal.sh

Universal cloud VM deployment orchestrator that works with any component.

**Usage:**
```bash
cd /path/to/your-component-a
./deploy-to-vm-universal.sh /path/to/infra-config.local.json [--no-cache] [--rebuild-volume]
```

**What it does:**
1. Auto-detects component ID from directory name (e.g., `ess-hub-a`, `kg-model-a`)
2. Finds the component's configuration in infra-config
3. Extracts VM deployment settings (vmHost, vmName, etc.)
4. Copies infra-config to VM at `/var/data/infra-config.local.json`
5. Syncs component code to VM at `/var/data/<component-id>/`
6. Stops existing containers
7. Runs `deploy.sh` on the VM with the proper config

**Flags:**
- `--no-cache`: Force Docker to rebuild images without cache
- `--rebuild-volume`: Remove and recreate all Docker volumes (fresh start)

**Examples:**
```bash
# Deploy ess-hub-a
cd /Users/pp3223/git/ess-hub-a
./deploy-to-vm-universal.sh /Users/pp3223/git/dev/infra-config.local.json

# Deploy kg-model-a with full rebuild
cd /Users/pp3223/git/kg-model-a
./deploy-to-vm-universal.sh /Users/pp3223/git/dev/infra-config.local.json --no-cache --rebuild-volume

# Deploy any component
cd /path/to/any-component-a
./deploy-to-vm-universal.sh /path/to/infra-config.local.json
```

### 2. cleanup-vm-universal.sh

Universal VM cleanup script that removes component files from VM root partition.

**Usage:**
```bash
cd /path/to/your-component-a
./cleanup-vm-universal.sh /path/to/infra-config.local.json
```

**What it does:**
1. Auto-detects component ID from directory name
2. Finds VM host from infra-config
3. Stops all component containers
4. Removes component files from `/opt/`, `/home/ubuntu/`, `/tmp/` (outside /var/data)
5. Cleans up stray config files
6. Reports disk space usage

**Example:**
```bash
cd /Users/pp3223/git/ess-hub-a
./cleanup-vm-universal.sh /Users/pp3223/git/dev/infra-config.local.json
```

## How It Works

### Auto-Detection

Both scripts automatically detect the component ID from the directory they're run in:

```bash
COMPONENT_ID="$(basename "$SCRIPT_DIR")"
```

If you run the script from `/Users/pp3223/git/ess-hub-a`, it detects `COMPONENT_ID="ess-hub-a"`.

### Configuration Lookup

The scripts find the component in infra-config:

```bash
# Find which system contains this component
SYSTEM_ID=$(jq -r --arg cid "$COMPONENT_ID" '
  .systems[] | 
  select(.components[]? | .componentId == $cid) | 
  .systemId
' "$CONFIG_FILE")

# Extract VM deployment settings
VM_HOST=$(jq -r '.systems[] | select(.systemId=="'$SYSTEM_ID'") | .components[] | select(.componentId=="'$COMPONENT_ID'") | .deployment.vmHost' "$CONFIG_FILE")
```

### COPILOT RULES Compliance

Both scripts follow COPILOT RULES for cloud deployment:

✅ **OFFICIAL-CLOUD-INFRA-CONFIG**: Always deploys config to `/var/data/infra-config.local.json`  
✅ **/var/data/ deployment**: All component files go to `/var/data/<component-id>/`  
✅ **Single Source of Truth**: Reads all configuration from central infra-config  
✅ **Universal pattern**: Works with any component without modification  

## Installation

### Option 1: Copy to Each Component

Copy the scripts to any component directory and make them executable:

```bash
cp deploy-to-vm-universal.sh /path/to/component-a/
cp cleanup-vm-universal.sh /path/to/component-a/
chmod +x /path/to/component-a/deploy-to-vm-universal.sh
chmod +x /path/to/component-a/cleanup-vm-universal.sh
```

### Option 2: Central Location with Symlinks

Place in a central location and create symlinks:

```bash
# Create central tools directory
mkdir -p ~/tools/vm-deployment

# Copy scripts
cp deploy-to-vm-universal.sh ~/tools/vm-deployment/
cp cleanup-vm-universal.sh ~/tools/vm-deployment/
chmod +x ~/tools/vm-deployment/*.sh

# Create symlinks in components
cd /path/to/component-a
ln -s ~/tools/vm-deployment/deploy-to-vm-universal.sh .
ln -s ~/tools/vm-deployment/cleanup-vm-universal.sh .
```

### Option 3: Add to PATH

Add the scripts directory to your PATH for global access:

```bash
# Add to ~/.zshrc or ~/.bashrc
export PATH="$HOME/tools/vm-deployment:$PATH"

# Then run from any component directory
cd /path/to/component-a
deploy-to-vm-universal.sh /path/to/infra-config.local.json
```

## Requirements

- **jq**: JSON query tool (`brew install jq` on macOS)
- **SSH key**: `~/.ssh/graxiom_core.pem` with proper permissions
- **infra-config**: Component must be registered in infra-config with:
  - `deployment.vmHost`: VM IP address
  - `deployment.vmName`: VM identifier
- **deploy.sh**: Component must have a deploy.sh that follows Universal Deployment Rule

## VM Requirements

- Ubuntu/Debian-based VM
- Docker and docker-compose installed
- `/var/data/` directory with write permissions for ubuntu user
- SSH access configured

## Troubleshooting

### Component Not Found

```
ERROR: Component 'xyz-a' not found in infra-config.
```

**Solution**: Ensure the component is registered in infra-config with matching componentId.

### SSH Connection Failed

```
ERROR: SSH connection failed to ubuntu@<VM_HOST>
```

**Solution**: 
- Verify SSH key exists: `ls -la ~/.ssh/graxiom_core.pem`
- Check VM is accessible: `ping <VM_HOST>`
- Test SSH manually: `ssh -i ~/.ssh/graxiom_core.pem ubuntu@<VM_HOST>`

### Missing vmHost

```
ERROR: Missing deployment.vmHost for xyz-a in infra-config.
This component may not be configured for cloud deployment.
```

**Solution**: Add VM deployment configuration to the component in infra-config:

```json
{
  "componentId": "xyz-a",
  "componentType": "AGENT",
  "deployment": {
    "vmHost": "34.254.221.79",
    "vmName": "VM-XYZ-BE",
    "port": 9001,
    ...
  }
}
```

## See Also

- **COPILOT-RULES.md**: Cloud deployment architecture rules
- **deploy.sh**: Component-specific deployment script pattern
- **infra-config.json**: Central configuration schema
